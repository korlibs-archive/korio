package com.soywiz.korio.ext.web.router

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.async
import com.soywiz.korio.async.invokeSuspend
import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.ds.OptByteBuffer
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.error.InvalidOperationException
import com.soywiz.korio.ext.web.sstatic.serveStatic
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.http.httpError
import com.soywiz.korio.serialization.ObjectMapper
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.querystring.QueryString
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.copyTo
import com.soywiz.korio.text.AsyncTextWriterContainer
import com.soywiz.korio.util.Dynamic
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.htmlspecialchars
import com.soywiz.korio.vfs.VfsFile
import java.io.FileNotFoundException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.nio.file.InvalidPathException
import kotlin.reflect.KClass

object RoutePriority {
	const val HIGHEST = -1000
	const val HIGH = -100
	const val NORMAL = 0
	const val LOW = 100
	const val LOWEST = 1000
}

annotation class WsRoute(val path: String, val priority: Int = RoutePriority.NORMAL)
annotation class Route(val method: Http.Methods, val path: String, val priority: Int = RoutePriority.NORMAL, val textContentType: String = "text/html")
annotation class Param(val name: String, val limit: Int = -1)
annotation class Get(val name: String, val limit: Int = -1)
annotation class Post(val name: String, val limit: Int = -1)
annotation class Header(val name: String, val limit: Int = -1)

val HttpServer.Request.extraParams by Extra.Property<MutableMap<String, String>> { lmapOf() }

fun HttpServer.Request.pathParam(name: String): String? {
	return this.extraParams[name]
}

fun HttpServer.Request.getParam(name: String): String? {
	return this.getParams[name]?.first()
}

open class KorBaseRoute(val bpath: String, val priority: Int) {
	val matchNames = ArrayList<String>()
	val regex = Regex(Regex.escapeReplacement(bpath)
		.replace("*", ".*")
		.replace(Regex(":(\\w+)")) {
			matchNames += it.groupValues[1]
			"([^/]+)"
		})

	open fun matches(req: HttpServer.BaseRequest): Boolean {
		return regex.matches(req.path)
	}
}

data class KorRoute(val method: Http.Method, val path: String, val bpriority: Int, val handler: suspend (HttpServer.Request) -> Unit) : KorBaseRoute(path, bpriority) {
	//init {
	//	println("-------------")
	//	println(path)
	//	println(Regex.escapeReplacement(path))
	//	println(matchNames)
	//	println(regex)
	//}

	suspend fun handle(req: HttpServer.Request) {
		val m = regex.find(req.path)!!
		for ((index, key) in matchNames.withIndex()) {
			val value = m.groupValues[index + 1]
			//println("extra:$key -> $value")
			req.extraParams[key] = com.soywiz.korio.serialization.querystring.URLDecoder.decode(value, "UTF-8")
		}

		handler(req)
	}

	fun matchesMethod(method: Http.Method?) = (this.method == Http.Methods.ALL) || (this.method == method)

	override fun matches(req: HttpServer.BaseRequest): Boolean = matchesMethod((req as? HttpServer.Request?)?.method) && super.matches(req)
}

data class KorWsRoute(val path: String, val bpriority: Int = 0, val handler: suspend (HttpServer.WsRequest) -> Unit) : KorBaseRoute(path, bpriority) {

}

class KorRouter(val injector: AsyncInjector, val requestConfig: HttpServer.RequestConfig) : Extra by Extra.Mixin() {
	val interceptors = ArrayList<suspend (HttpServer.Request, Map<String, String>) -> Unit>()
	val httpRoutes = ArrayList<KorRoute>()
	val wsRoutes = ArrayList<KorWsRoute>()
	var dirty = false

	fun route(method: Http.Method, path: String, priority: Int = 0, handler: suspend (HttpServer.Request) -> Unit): KorRouter {
		httpRoutes += KorRoute(method, path, priority, handler)
		dirty = true
		return this
	}

	fun wsroute(path: String, priority: Int = 0, handler: suspend (HttpServer.WsRequest) -> Unit): KorRouter {
		wsRoutes += KorWsRoute(path, priority, handler)
		dirty = true
		return this
	}

	suspend fun accept(req: HttpServer.BaseRequest) {
		if (dirty) {
			dirty = false
			httpRoutes.sortBy { it.priority }
			wsRoutes.sortBy { it.priority }
		}
		when (req) {
			is HttpServer.Request -> {
				val route = httpRoutes.firstOrNull { it.matches(req) }
				if (route != null) {
					route.handle(req)
				} else {
					req.setStatus(404)
					req.addHeader("Content-Type", "text/html")
					req.end("Route not found for ${req.path.htmlspecialchars()}")
				}
			}
			is HttpServer.WsRequest -> {
				val route = wsRoutes.firstOrNull { it.matches(req) }
				if (route != null) {
					route.handler(req)
				} else {
					req.reject()
				}
			}
		}
	}
}

suspend inline fun <reified T : Any> KorRouter.registerRoutes() = this.apply { this.registerRoutes(T::class) }
//suspend fun HttpServer.router(router: KorRouter) = this.allHandler { router.accept(it) }

suspend fun HttpServer.router(injector: AsyncInjector = AsyncInjector(), configurer: suspend KorRouter.() -> Unit): HttpServer {
	val router = KorRouter(injector, requestConfig)
	router.configurer()
	this.allHandler { router.accept(it) }
	return this
}

suspend fun KorRouter.registerInterceptor(interceptor: suspend (HttpServer.Request, Map<String, String>) -> Unit) {
	interceptors.add(interceptor)
}

private val MAX_BODY_SIZE = 16 * 1024

suspend private fun registerHttpRoute(router: KorRouter, instance: Any, method: Method, route: Route) {
	//object : Continuation<Any>
	//router.route(route.path).handler { rreq ->
	//router.router.route(route.method, route.path).handler(BodyHandler.create().setBodyLimit(16 * 1024)).handler { rreq ->
	router.route(route.method, route.path, route.priority) { rreq ->
		val res = rreq
		val req = rreq
		//val res = rreq.response()
		//val req = rreq.request()
		val headers = req.headers
		val contentType = headers["Content-Type"]

		val bodyHandler = com.soywiz.korio.async.Promise.Deferred<Unit>()

		var postParams = mapOf<String, List<String>>()
		var totalRequestSize = 0L
		var bodyOverflow = false
		val bodyContent = OptByteBuffer()
		val response = Http.Response()
		val request = Http.Request(rreq.uri, req.headers)

		req.handler {
			totalRequestSize += it.size
			if (it.size + bodyContent.size < MAX_BODY_SIZE) {
				bodyContent.append(it)
			} else {
				bodyOverflow = true
			}
		}

		req.endHandler {
			try {
				if ("application/x-www-form-urlencoded" == contentType) {
					if (!bodyOverflow) {
						postParams = QueryString.decode(bodyContent.toString(Charsets.UTF_8))
					}
				}
			} finally {
				bodyHandler.resolve(Unit)
			}
		}

		async {
			val mapper = router.injector.getOrNull<ObjectMapper>() ?: ObjectMapper()

			try {
				bodyHandler.promise.await()

				if (bodyOverflow) throw RuntimeException("Too big request payload: $totalRequestSize")

				//var deferred: Promise.Deferred<Any>? = null
				val args = ArrayList<Any?>()
				val mapArgs = lmapOf<String, String>()
				for ((indexedParamType, annotations) in method.parameterTypes.withIndex().zip(method.parameterAnnotations)) {
					val (index, paramType) = indexedParamType
					val param = annotations.filterIsInstance<Param>().firstOrNull()
					val get = annotations.filterIsInstance<Get>().firstOrNull()
					val post = annotations.filterIsInstance<Post>().firstOrNull()
					val header = annotations.filterIsInstance<Header>().firstOrNull()
					when {
						param != null -> {
							args += Dynamic.dynamicCast(rreq.pathParam(param.name), paramType)
						}
						get != null -> {
							args += Dynamic.dynamicCast(rreq.getParam(get.name), paramType)
						}
						post != null -> {
							val result = postParams[post.name]?.firstOrNull()
							mapArgs[post.name] = result ?: ""
							args += Dynamic.dynamicCast(result, paramType)
						}
						header != null -> {
							args += Dynamic.dynamicCast(req.getHeader(header.name) ?: "", paramType)
						}
						Http.Auth::class.java.isAssignableFrom(paramType) -> {
							args += Http.Auth.parse(req.getHeader("authorization") ?: "")
						}
						Http.Headers::class.java.isAssignableFrom(paramType) -> {
							args += Http.Headers(headers.toList()) as Any?
						}
						Http.Response::class.java.isAssignableFrom(paramType) -> {
							args += response
						}
						Http.Request::class.java.isAssignableFrom(paramType) -> {
							args += request
						}
						HttpServer.Request::class.java.isAssignableFrom(paramType) -> {
							args += rreq
						}
						Continuation::class.java.isAssignableFrom(paramType) -> {
							//deferred = Promise.Deferred<Any>()
							//args += deferred.toContinuation()
						}
						else -> {
							httpError(500, "Route $route expected Http.Headers type, or @Get, @Post or @Header annotation for parameter $index in method ${method.name}")
						}
					}
				}

				for (interceptor in router.interceptors) {
					interceptor(req, mapArgs)
				}

				res.setStatus(200)
				res.replaceHeader("Content-Type", route.textContentType)
				val result = method.invokeSuspend(instance, args)

				val finalResult = if (result is Promise<*>) result.await() else result

				for ((k, v) in response.headers) res.addHeader(k, v)

				when (finalResult) {
					null -> res.end("")
					is String -> res.end("$finalResult")
					is AsyncStream -> {
						res.replaceHeader("Content-Length", "${finalResult.size()}")
						finalResult.copyTo(res)
						res.close()
					}
					is VfsFile -> {
						res.serveStatic(finalResult)
					}
				// @TODO: Korte should allow to return this interface
					is AsyncTextWriterContainer -> {
						// @TODO: Use chunked to avoid wasting memory!
						val buffer = StringBuffer()
						finalResult.write { buffer.append(it) }
						res.end(buffer.toString())
					}
					else -> {
						res.replaceHeader("Content-Type", "application/json")
						res.end(Json.encode(finalResult, mapper))
					}
				}
			} catch (tt: Throwable) {
				val t = when (tt) {
					is InvocationTargetException -> tt.cause ?: tt
					else -> tt
				}

				when (t) {
					is Http.RedirectException -> {
						System.err.println("Redirected: ${t.statusCode}:${t.statusText}: Location: ${t.redirectUri}")
						res.setStatus(t.statusCode, t.statusText)
						res.replaceHeader("Location", t.redirectUri)
						res.end()
					}
					else -> {
						tt.printStackTrace() // @TODO: Enable just in debug
						val ft = when (t) {
							is java.nio.file.NoSuchFileException,
							is InvalidPathException,
							is FileNotFoundException,
							is NoSuchFileException,
							is NoSuchElementException
							->
								//Http.HttpException(404, t2.message ?: "")
								Http.HttpException(404, "404 - Not Found - ${req.path}")
							is InvalidOperationException ->
								Http.HttpException(400, "400 - Invalid Operation - ${req.path}")
							else -> {
								System.err.println("OtherException: ### ${req.absoluteURI} : $postParams")
								tt.printStackTrace()
								//Http.HttpException(500, t2.message ?: "")
								Http.HttpException(500, "500 - Internal Server Error")
							}
						}
						System.err.println("Http.HttpException: +++ ${ft.statusCode}:${ft.statusText} : ${req.absoluteURI} : $postParams")
						res.setStatus(ft.statusCode, ft.statusText)
						res.replaceHeader("Content-Type", "text/html")
						for (header in ft.headers) res.addHeader(header.first, header.second)
						res.end(ft.msg)
					}
				}
			}
		}
	}
}

suspend private fun registerWsRoute(router: KorRouter, instance: Any, method: Method, route: WsRoute) {
	router.wsroute(route.path, route.priority) { ws ->
		val args = ArrayList<Any?>()
		for ((indexedParamType, annotations) in method.parameterTypes.withIndex().zip(method.parameterAnnotations)) {
			val (index, paramType) = indexedParamType
			when {
				HttpServer.WsRequest::class.java.isAssignableFrom(paramType) -> {
					args += ws
				}
				com.soywiz.korio.coroutine.Continuation::class.java.isAssignableFrom(paramType) -> {
				}
				else -> {
					args.add(null)
				}

			}
		}
		method.invokeSuspend(instance, args)
	}
}

suspend fun <T : Any> KorRouter.registerRoutes(clazz: KClass<T>) {
	val router = this@registerRoutes

	println("Registering route $clazz...")

	val instance = injector.get<T>(clazz)

	//println("   [1]")

	//val eventLog = injector.get<EventLog>()

	//println("   [2]")

	//for (m in clazz.kotlin.members) {
	//	println(m)
	//}

	for (method in clazz.java.declaredMethods) {
		//println("   method: $method")
		val route = method.getAnnotation(Route::class.java)
		if (route != null) {
			println(" - Registered http: $route")
			registerHttpRoute(router, instance, method, route)
		}
		val wsroute = method.getAnnotation(WsRoute::class.java)
		if (wsroute != null) {
			println(" - Registered ws: $wsroute")
			registerWsRoute(router, instance, method, wsroute)
		}
	}

	println("Ok")
}
