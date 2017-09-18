package com.soywiz.korio.ext.web.cookie

import com.soywiz.korio.ext.web.router.KorRouter
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.util.Extra

data class KorCookies(
	val items: LinkedHashMap<String, String?>
) {
	companion object {
		fun parseSetCookies(setCookies: List<String>): KorCookies {
			val out = LinkedHashMap<String, String?>()
			for (setCookie in setCookies) {
				val parts = setCookie.split(';')
				for (part in parts) {
					val tpart = part.trim()
					val keyvalue = tpart.split('=', limit = 2)
					val key = keyvalue[0]
					val value = keyvalue.getOrElse(1) { null }
					out[key] = value
				}
			}
			return KorCookies(out)
		}
	}

	override fun toString(): String = items.entries.map {
		if (it.value != null) "${it.key}=${it.value}" else it.key
	}.joinToString("; ")
}

val HttpServer.Request.cookies by Extra.PropertyThis<HttpServer.Request, KorCookies> {
	KorCookies.parseSetCookies(this.getHeaderList("Set-Cookie"))
}

fun HttpServer.RequestConfig.supportCookies() = this.apply {
	beforeSendHeadersInterceptors += { req ->
		val str = req.cookies.toString()
		if (str.isNotBlank()) {
			req.addHeader("Cookie", str)
		}
	}
}

fun HttpServer.supportCookies() = this.apply { requestConfig.supportCookies() }
fun KorRouter.supportCookies() = this.apply { requestConfig.supportCookies() }

