package com.soywiz.korio.vertx.router

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.async
import com.soywiz.korio.async.invokeSuspend
import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.error.InvalidOperationException
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.httpError
import com.soywiz.korio.util.Dynamic
import io.netty.handler.codec.http.QueryStringDecoder
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.Json
import java.lang.reflect.InvocationTargetException

annotation class Route(val method: HttpMethod, val path: String)

annotation class Param(val name: String, val limit: Int = -1)
annotation class Post(val name: String, val limit: Int = -1)

suspend inline fun <reified T : Any> KorRouter.registerRouter() = this.registerRouter(T::class.java)

suspend fun KorRouter.registerInterceptor(interceptor: suspend (HttpServerRequest, Map<String, String>) -> Unit) {
	interceptors.add(interceptor)
}

private val MAX_BODY_SIZE = 16 * 1024

suspend fun KorRouter.registerRouter(clazz: Class<*>) {
	val router = this

	println("Registering route $clazz...")

	val instance = injector.get(clazz)

	//println("   [1]")

	//val eventLog = injector.get<EventLog>()

	//println("   [2]")

	//for (m in clazz.kotlin.members) {
	//	println(m)
	//}

	for (method in clazz.declaredMethods) {
		//println("   method: $method")
		val route = method.getAnnotation(Route::class.java)
		if (route != null) {
			//object : Continuation<Any>
			//router.route(route.path).handler { rreq ->
			//router.router.route(route.method, route.path).handler(BodyHandler.create().setBodyLimit(16 * 1024)).handler { rreq ->
			router.router.route(route.method, route.path).handler { rreq ->
				val res = rreq.response()

				val req = rreq.request()
				val contentType = req.headers().get("Content-Type")

				val bodyHandler = Promise.Deferred<Unit>()

				var postParams = mapOf<String, List<String>>()
				var totalRequestSize = 0L
				var bodyOverflow = false
				val bodyContent = Buffer.buffer()

				req.handler {
					totalRequestSize += it.length()
					if (it.length() + bodyContent.length() < MAX_BODY_SIZE) {
						bodyContent.appendBuffer(it)
					} else {
						bodyOverflow = true
					}
				}

				req.endHandler {
					try {
						if ("application/x-www-form-urlencoded" == contentType) {
							if (!bodyOverflow) {
								val qsd = QueryStringDecoder(bodyContent.toString(), false)
								postParams = qsd.parameters()
							}
						}
					} finally {
						bodyHandler.resolve(Unit)
					}
				}

				async {
					try {
						bodyHandler.promise.await()

						if (bodyOverflow) throw RuntimeException("Too big request payload: $totalRequestSize")

						//var deferred: Promise.Deferred<Any>? = null
						val args = arrayListOf<Any?>()
						val mapArgs = hashMapOf<String, String>()
						for ((indexedParamType, annotations) in method.parameterTypes.withIndex().zip(method.parameterAnnotations)) {
							val (index, paramType) = indexedParamType
							val get = annotations.filterIsInstance<Param>().firstOrNull()
							val post = annotations.filterIsInstance<Post>().firstOrNull()
							if (get != null) {
								args += Dynamic.dynamicCast(rreq.pathParam(get.name), paramType)
							} else if (post != null) {
								val result = postParams[post.name]?.firstOrNull()
								mapArgs[post.name] = result ?: ""
								args += Dynamic.dynamicCast(result, paramType)
							} else if (Continuation::class.java.isAssignableFrom(paramType)) {
								//deferred = Promise.Deferred<Any>()
								//args += deferred.toContinuation()
							} else {
								httpError(500, "Route $route expected @Get or @Post annotation for parameter $index in method ${method.name}")
							}
						}

						for (interceptor in interceptors) {
							interceptor(req, mapArgs)
						}

						val result = method.invokeSuspend(instance, args)

						val finalResult = if (result is Promise<*>) result.await() else result

						when (finalResult) {
							is String -> res.end("$finalResult")
							else -> res.end(Json.encode(finalResult))
						}
					} catch (t: Throwable) {
						val t2 = when (t) {
							is InvocationTargetException -> t.cause ?: t
							else -> t
						}
						val ft = when (t2) {
							is NoSuchElementException -> Http.HttpException(404, t2.message ?: "")
							is InvalidOperationException -> Http.HttpException(400, t2.message ?: "")
							else -> t2
						}
						if (ft is Http.HttpException) {
							System.err.println("+++ ${req.absoluteURI()} : $postParams")
							res.statusCode = ft.statusCode
							res.statusMessage = ft.statusText
						} else {
							System.err.println("### ${req.absoluteURI()} : $postParams")
							t.printStackTrace()
							res.statusCode = 500
						}
						res.end("${ft.message}")
					}
				}
			}
		}
	}

	println("Ok")
}
