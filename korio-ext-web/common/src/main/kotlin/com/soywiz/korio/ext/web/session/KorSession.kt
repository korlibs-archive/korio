package com.soywiz.korio.ext.web.session

import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.ext.web.cookie.KorCookies
import com.soywiz.korio.ext.web.cookie.cookies
import com.soywiz.korio.ext.web.cookie.registerCookies
import com.soywiz.korio.ext.web.router.KorRouter
import com.soywiz.korio.lang.Dynamic
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.UUID

abstract class SessionProvider {
	abstract suspend fun get(sessionId: String): String?
	abstract suspend fun set(sessionId: String, content: String)
}

/**
 * @TODO: Warning: Right now this implementation leaks since it doesn't delete sessions ta all.
 * Also an in-memory session provider should be use just for debugging purposes in local development.
 */
class MemorySessionProvider : SessionProvider() {
	val memory = lmapOf<String, String>()

	suspend override fun get(sessionId: String): String? = memory[sessionId]
	suspend override fun set(sessionId: String, content: String) {
		memory[sessionId] = content
	}
}

class KorSession(
	private val cookies: KorCookies,
	private val sessionProvider: SessionProvider,
	private val request: HttpServer.Request
) {
	private val korSessionCookie = cookies.getCookieSure("korSessionId")
	private val korSessionId = korSessionCookie.getOrSetValue { UUID.randomUUID().toString() }
	private var prepared = false
	private var obj: Any? = null

	init {
		request.finalizers += {
			store()
		}
	}

	suspend private fun prepareOnce() {
		if (prepared) return
		prepared = true
		obj = Json.decode(sessionProvider.get(korSessionId) ?: "{}")
	}

	suspend private fun store() {
		sessionProvider.set(korSessionId, Json.encodeUntyped(obj))
	}

	suspend fun getOrSet(key: String, default: () -> Any?): Any? {
		val res = get(key)
		if (res == null) {
			val value = default()
			set(key, value)
			return value
		} else {
			return res
		}
	}

	suspend fun get(key: String): Any? {
		prepareOnce()
		return Dynamic.get(obj, key)
	}

	suspend fun set(key: String, value: Any?) {
		prepareOnce()
		if (obj == null) obj = lmapOf<String, Any?>()
		Dynamic.set(obj, key, value)
	}

	suspend fun getInt(key: String): Int = Dynamic.toInt(get(key))
	suspend fun setInt(key: String, value: Int) = set(key, value)

	suspend fun getLong(key: String): Long = Dynamic.toLong(get(key))
	suspend fun setLong(key: String, value: Long) = set(key, value)

	suspend fun getStringOrNull(key: String): String? = Dynamic.toStringOrNull(get(key))
	suspend fun getString(key: String): String = Dynamic.toString(get(key))
	suspend fun setString(key: String, value: String) = set(key, value)

	suspend fun getBool(key: String): Boolean = Dynamic.toBool(get(key))
	suspend fun setBool(key: String, value: Boolean) = set(key, value)
}

var HttpServer.RequestConfig.sessionProvider: SessionProvider by Extra.Property { MemorySessionProvider() }

val HttpServer.Request.session by Extra.PropertyThis<HttpServer.Request, KorSession> {
	KorSession(this.cookies, this.requestConfig.sessionProvider, this)
}

suspend fun HttpServer.RequestConfig.registerSessions(provider: SessionProvider) = this.apply {
	registerCookies()
	sessionProvider = provider
}

suspend fun HttpServer.registerSessions(provider: SessionProvider) = this.apply { requestConfig.registerSessions(provider) }
suspend fun KorRouter.registerSessions(provider: SessionProvider = MemorySessionProvider()) = this.apply { requestConfig.registerSessions(provider) }

