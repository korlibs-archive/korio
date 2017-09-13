package com.soywiz.korio.ext.web.router

import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer

data class KorRoute(val method: Http.Method, val path: String, val handle: (HttpServer.Request) -> Unit) {
	fun matches(req: HttpServer.Request): Boolean {
		if (req.method == method && req.uri == path) {
			return true
		}
		return false
	}
}

class KorRouter(val injector: AsyncInjector) {
	var interceptors = arrayListOf<suspend (HttpServer.Request, Map<String, String>) -> Unit>()
	val routes = arrayListOf<KorRoute>()

	fun route(method: Http.Method, path: String, handler: (HttpServer.Request) -> Unit): KorRouter {
		routes += KorRoute(method, path, handler)
		return this
	}

	fun handle(req: HttpServer.Request) {
		val route = routes.firstOrNull { it.matches(req) }
		if (route != null) {
			route.handle(req)
		} else {
			req.putHeader("Content-Type", "text/html")
			req.end("Route not found for ${req.uri}")
		}
	}
}
