package com.soywiz.korio.ext.web.router

import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.util.Extra

val HttpServer.Request.extraParams by Extra.Property<HashMap<String, String>> { LinkedHashMap() }

fun HttpServer.Request.pathParam(name: String): String? {
	return this.extraParams[name]
}

data class KorRoute(val method: Http.Method, val path: String, val handler: (HttpServer.Request) -> Unit) {
	val matchNames = arrayListOf<String>()
	val regex = Regex(Regex.escapeReplacement(path).replace("\\*", ".*").replace(Regex(":(\\w+)")) {
		matchNames += it.groupValues[1]
		"([^/]+)"
	})

	//init {
	//	println("-------------")
	//	println(path)
	//	println(Regex.escapeReplacement(path))
	//	println(matchNames)
	//	println(regex)
	//}

	fun handle(req: HttpServer.Request) {
		val m = regex.find(req.uri)!!
		for ((index, key) in matchNames.withIndex()) {
			val value = m.groupValues[index + 1]
			//println("extra:$key -> $value")
			req.extraParams[key] = value
		}

		handler(req)
	}

	fun matches(req: HttpServer.Request): Boolean {
		if (req.method == method && regex.matches(req.uri)) {
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

	fun accept(req: HttpServer.Request) {
		val route = routes.firstOrNull { it.matches(req) }
		if (route != null) {
			route.handle(req)
		} else {
			req.putHeader("Content-Type", "text/html")
			req.end("Route not found for ${req.uri}")
		}
	}
}
