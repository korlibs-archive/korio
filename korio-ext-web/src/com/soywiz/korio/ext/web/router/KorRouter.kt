package com.soywiz.korio.ext.web.router

import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.util.Extra
import java.net.URLDecoder

val HttpServer.Request.extraParams by Extra.Property<HashMap<String, String>> { LinkedHashMap() }

fun HttpServer.Request.pathParam(name: String): String? {
	return this.extraParams[name]
}

open class KorBaseRoute(val bpath: String) {
	val matchNames = arrayListOf<String>()
	val regex = Regex(Regex.escapeReplacement(bpath).replace("\\*", ".*").replace(Regex(":(\\w+)")) {
		matchNames += it.groupValues[1]
		"([^/]+)"
	})

	open fun matches(req: HttpServer.BaseRequest): Boolean = regex.matches(req.uri)
}

data class KorRoute(val method: Http.Method, val path: String, val handler: suspend (HttpServer.Request) -> Unit) : KorBaseRoute(path) {
	//init {
	//	println("-------------")
	//	println(path)
	//	println(Regex.escapeReplacement(path))
	//	println(matchNames)
	//	println(regex)
	//}

	suspend fun handle(req: HttpServer.Request) {
		val m = regex.find(req.uri)!!
		for ((index, key) in matchNames.withIndex()) {
			val value = m.groupValues[index + 1]
			//println("extra:$key -> $value")
			req.extraParams[key] = URLDecoder.decode(value, "UTF-8")
		}

		handler(req)
	}

	override fun matches(req: HttpServer.BaseRequest): Boolean = (req as? HttpServer.Request?)?.method == method && super.matches(req)
}

data class KorWsRoute(val path: String, val handler: suspend (HttpServer.WsRequest) -> Unit) : KorBaseRoute(path) {

}

class KorRouter(val injector: AsyncInjector) {
	var interceptors = arrayListOf<suspend (HttpServer.Request, Map<String, String>) -> Unit>()
	val httpRoutes = arrayListOf<KorRoute>()
	val wsRoutes = arrayListOf<KorWsRoute>()

	fun route(method: Http.Method, path: String, handler: suspend (HttpServer.Request) -> Unit): KorRouter {
		httpRoutes += KorRoute(method, path, handler)
		return this
	}

	fun wsroute(path: String, handler: suspend (HttpServer.WsRequest) -> Unit): KorRouter {
		wsRoutes += KorWsRoute(path, handler)
		return this
	}

	suspend fun accept(req: HttpServer.BaseRequest) {
		when (req) {
			is HttpServer.Request -> {
				val route = httpRoutes.firstOrNull { it.matches(req) }
				if (route != null) {
					route.handle(req)
				} else {
					req.putHeader("Content-Type", "text/html")
					req.end("Route not found for ${req.uri}")
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
