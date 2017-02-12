package com.soywiz.korio.vertx.router

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.async
import com.soywiz.korio.async.invokeSuspend
import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.util.Dynamic
import io.netty.handler.codec.http.QueryStringDecoder
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
			router.router.route(route.method, route.path).handler { rreq ->
				val res = rreq.response()

				val req = rreq.request()
				val contentType = req.headers().get("Content-Type")

				val bodyHandler = Promise.Deferred<Map<String, List<String>>>()

				req.bodyHandler { buf ->
					if ("application/x-www-form-urlencoded" == contentType) {
						val qsd = QueryStringDecoder(buf.toString(), false)
						val params = qsd.parameters()
						bodyHandler.resolve(params)
					}
				}

				async {
					try {
						//var deferred: Promise.Deferred<Any>? = null
						val args = arrayListOf<Any?>()
						val mapArgs = hashMapOf<String, String>()
						for ((paramType, annotations) in method.parameterTypes.zip(method.parameterAnnotations)) {
							val get = annotations.filterIsInstance<Param>().firstOrNull()
							val post = annotations.filterIsInstance<Post>().firstOrNull()
							if (get != null) {
								args += Dynamic.dynamicCast(rreq.pathParam(get.name), paramType)
							} else if (post != null) {
								val postParams = bodyHandler.promise.await()
								val result = postParams[post.name]?.firstOrNull()
								mapArgs[post.name] = result ?: ""
								args += Dynamic.dynamicCast(result, paramType)
							} else if (Continuation::class.java.isAssignableFrom(paramType)) {
								//deferred = Promise.Deferred<Any>()
								//args += deferred.toContinuation()
							} else {
								throw RuntimeException("Expected @Get annotation")
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
						res.statusCode = 500
						val t2 = when (t) {
							is InvocationTargetException -> t.cause ?: t
							else -> {
								//println("Router.registerRouterAsync (${t.message}):")
								//t.printStackTrace()
								t
							}
						}
						res.end("${t2.message}")
					}
				}
			}
		}
	}

	println("Ok")
}
