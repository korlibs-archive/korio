package com.soywiz.korio.net.http

import com.soywiz.korio.ds.MapList
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import java.nio.charset.Charset
import java.util.*

open class HttpClient protected constructor() {
	enum class Method { HEAD, POST, GET, PUT, DELETE, OPTIONS, TRACE }


	data class Headers(val items: MapList<String, String> = MapList()) : Iterable<Pair<String, String>> {
		//class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>
		override fun iterator(): Iterator<Pair<String, String>> = items.flatMapIterator()

		constructor(map: Map<String, String>) : this(MapList(map.entries.map { it.key to it.value }))
		constructor(str: String?) : this(parse(str))

		operator fun get(key: String): String? = items.getFirst(key)

		companion object {
			fun fromListMap(map: Map<String?, List<String>>): Headers {
				return Headers(MapList(map.flatMap { pair -> if (pair.key == null) listOf() else pair.value.map { value -> pair.key!! to value } }))
			}

			fun parse(str: String?): MapList<String, String> {
				if (str == null) return MapList()
				return MapList(str.split("\n").map {
					val parts = it.trim().split(':', limit = 2)
					if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
				}.filterNotNull())
			}
		}

		fun withAppendedHeaders(vararg newHeaders: Pair<String, String>): Headers = Headers(MapList(this.items).appendAll(*newHeaders))
		fun withReplaceHeaders(vararg newHeaders: Pair<String, String>): Headers = Headers(MapList(this.items).replaceAll(*newHeaders))

		override fun toString(): String = "Headers(${items.joinToString(", ")})"
	}

	data class Response(
			val status: Int,
			val statusText: String,
			val headers: Headers,
			val content: AsyncInputStream
	) {
		val success = status < 400
		suspend fun readAllBytes() = content.readAll()

		fun withStringResponse(str: String, charset: Charset = Charsets.UTF_8) = this.copy(content = str.toByteArray(charset).openAsync())
	}

	suspend open fun request(method: Method, url: String, headers: Headers = Headers(), content: AsyncStream? = null): Response {
		TODO()
	}

	companion object {
		operator fun invoke() = httpClientFactory.create()
	}
}

class LogHttpClient : HttpClient() {
	val log = arrayListOf<String>()
	var response = HttpClient.Response(200, "OK", Headers(), "LogHttpClient.response".toByteArray(Charsets.UTF_8).openAsync())

	suspend override fun request(method: Method, url: String, headers: Headers, content: AsyncStream?): Response {
		val contentString = content?.readAll()?.toString(Charsets.UTF_8)
		log += "$method, $url, $headers, $contentString"
		return response
	}

	fun getAndClearLog() = log.toList().apply { log.clear() }
}

open class HttpClientFactory {
	open fun create(): HttpClient = object : HttpClient() {}
}

val httpClientFactory by lazy {
	ServiceLoader.load(HttpClientFactory::class.java).firstOrNull() ?: invalidOp("Can't find implementation for ${HttpClientFactory::class.java.name}")
}

fun createHttpClient() = httpClientFactory.create()