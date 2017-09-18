package com.soywiz.korio.ext.web.cookie

import com.soywiz.korio.ext.web.router.KorRouter
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.util.Extra
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

// http://www.faqs.org/rfcs/rfc2109.html
data class KorCookie(
	var name: String,
	var value: String? = null,
	var expire: Long = 0L,
	var maxAge: Long = 0L,
	var path: String? = null,
	var domain: String? = null,
	var secure: Boolean = false,
	var httpOnly: Boolean = false,
	var sameSiteStrict: Boolean? = null
) {
	fun getOrSetValue(default: () -> String): String {
		if (value == null) value = default()
		return value!!
	}

	companion object {
		fun parse(str: String): KorCookie {
			val parts = str.split(';').map { it.trim() }
			val cookie = KorCookie("", null)
			for ((index, part) in parts.withIndex()) {
				val pp = part.split('=', limit = 2).map { it.trim() }
				val key = pp[0]
				val value = pp.getOrNull(1)
				if (index == 0) {
					cookie.name = key
					cookie.value = value
				} else {
					when (key.toLowerCase()) {
					//"expires" ->expires
					}
				}
			}
			return cookie
		}
	}

	override fun toString(): String {
		val out = StringBuilder()
		out.append("$name=$value")
		if (expire != 0L) out.append("; Expires=${expire}")
		if (maxAge != 0L) out.append("; Max-Age=${maxAge}")
		if (domain != null) out.append("; Domain=$domain")
		if (path != null) out.append("; Path=$path")
		if (secure) out.append("; Secure")
		if (httpOnly) out.append("; HttpOnly")
		if (sameSiteStrict != null) out.append("; SameSite=${if (sameSiteStrict!!) "Strict" else "Lax"}")
		return out.toString()
	}
}

data class KorCookies(
	val items: HashMap<String, KorCookie>
) {
	fun getCookieSure(name: String): KorCookie {
		return items.getOrPut(name) { KorCookie(name) }
	}

	companion object {
		operator fun invoke(vararg cookies: KorCookie): KorCookies {
			return KorCookies(LinkedHashMap(cookies.map { it.name to it }.toMap()))
		}

		fun parseCookies(cookies: List<String>): KorCookies {
			val out = LinkedHashMap<String, KorCookie>()
			for (cookie in cookies) {
				val c = KorCookie.parse(cookie)
				out[c.name] = c
			}
			return KorCookies(out)
		}
	}

	override fun toString(): String = items.values.joinToString("\n") { "Set-Cookie: ${it.toString()}" }
}

val HttpServer.Request.cookies by Extra.PropertyThis<HttpServer.Request, KorCookies> {
	KorCookies.parseCookies(this.getHeaderList("Cookie"))
}

suspend fun HttpServer.RequestConfig.registerCookies() = this.apply {
	beforeSendHeadersInterceptors["cookies"] = { req ->
		for (cookie in req.cookies.items.values) {
			val cookieString = cookie.toString()
			req.addHeader("Set-Cookie", cookieString)
		}
	}
}

suspend fun HttpServer.registerCookies() = this.apply { requestConfig.registerCookies() }
suspend fun KorRouter.registerCookies() = this.apply { requestConfig.registerCookies() }

