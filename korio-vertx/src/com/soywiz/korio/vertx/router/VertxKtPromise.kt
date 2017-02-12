package com.soywiz.korio.vertx.router

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.async
import com.soywiz.korio.async.await
import com.soywiz.korio.async.tasksInProgress
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.util.Cancellable
import com.soywiz.korio.vertx.vertx
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

fun Router.get(path: String, callback: (RoutingContext) -> Unit): Route {
    return this.get(path).handler { callback(it) }
}

fun RoutingContext.header(key: String, value: String) {
    this.response().putHeader(key, value)
}

fun Vertx.router(callback: Router.() -> Unit): Router {
    val router = Router.router(this)
    router.callback()
    return router
}

class KorRouter(val injector: AsyncInjector, val router: Router) {
	var interceptors = arrayListOf<suspend (HttpServerRequest, Map<String, String>) -> Unit>()
}

suspend fun routedHttpServer(injector: AsyncInjector, port: Int = System.getenv("PORT")?.toIntOrNull() ?: 8080, callback: suspend KorRouter.() -> Unit = { }): Cancellable {
    tasksInProgress.addAndGet(1)

    val router = Router.router(vertx)
    val korRouter = KorRouter(injector, router)

    println("Preparing router...")
    callback.await(korRouter)
    println("Preparing router...Ok")

    val deferred = Promise.Deferred<Unit>()

    val httpServer = vertx.createHttpServer()
            .requestHandler { req -> router.accept(req) }
            .listen(port) {
                if (it.failed()) {
                    println("routedHttpServerAsync (${it.cause().message}):")
                    it.cause().printStackTrace()
                    deferred.reject(it.cause())
                } else {
                    println("Listening at port ${it.result().actualPort()}")
                    deferred.resolve(Unit)
                }
            }

    deferred.promise.await()

    return Cancellable {
        httpServer.close()
        tasksInProgress.addAndGet(-1)
    }
}
