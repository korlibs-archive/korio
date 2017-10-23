package com.soywiz.korio.ext.web.router

import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.serialization.querystring.URLDecoder
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.htmlspecialchars

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
annotation class RawBody
annotation class JsonContent
annotation class Header(val name: String, val limit: Int = -1)

val HttpServer.Request.extraParams by Extra.Property<MutableMap<String, String>> { lmapOf() }

suspend fun HttpServer.Request.pathParam(name: String): String? = this.extraParams[name]
suspend fun HttpServer.Request.getParam(name: String): String? = this.getParams[name]?.first()

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
			req.extraParams[key] = URLDecoder.decode(value, "UTF-8")
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

fun KorRouter.get(path: String, priority: Int = 0, handler: suspend (HttpServer.Request) -> Unit) = this.route(Http.Method.GET, path, priority, handler)
fun KorRouter.post(path: String, priority: Int = 0, handler: suspend (HttpServer.Request) -> Unit) = this.route(Http.Method.POST, path, priority, handler)
fun KorRouter.all(path: String, priority: Int = 0, handler: suspend (HttpServer.Request) -> Unit) = this.route(Http.Methods.ALL, path, priority, handler)

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

val MAX_BODY_SIZE = 16 * 1024

