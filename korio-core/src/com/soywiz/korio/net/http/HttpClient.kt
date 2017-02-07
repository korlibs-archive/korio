package com.soywiz.korio.net.http

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.*
import java.util.*

open class HttpClient protected constructor() {
	enum class Method { HEAD, POST, GET, PUT, DELETE, OPTIONS, TRACE }

	class Headers(val items: List<Pair<String, String>> = listOf()) : Iterable<Pair<String, String>> {
		//class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>
		override fun iterator(): Iterator<Pair<String, String>> = items.iterator()

		constructor(map: Map<String, String>) : this(map.entries.map { it.key to it.value })
		constructor(str: String?) : this(parse(str))

		val byKey by lazy {
			items.groupBy { it.first.toLowerCase() }.map { it.key to it.value.map { it.second } }.toMap()
		}

		operator fun get(key: String): String? = byKey[key.trim().toLowerCase()]?.firstOrNull()

		companion object {
			fun fromListMap(map: Map<String?, List<String>>): Headers {
				return Headers(map.flatMap { pair -> if (pair.key == null) listOf() else pair.value.map { value -> pair.key!! to value } })
			}

			fun parse(str: String?): List<Pair<String, String>> {
				if (str == null) return listOf()
				return str.split("\n").map {
					val parts = it.trim().split(':', limit = 2)
					if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
				}.filterNotNull()
			}
		}

		override fun toString(): String = "Headers(${items.joinToString(", ")})"
	}

	class Response(
			val status: Int,
			val statusText: String,
			val headers: Headers,
			val content: AsyncInputStream
	) {
		val success = status < 400
		suspend fun readAllBytes() = content.readAll()
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
}

open class HttpClientFactory {
	open fun create(): HttpClient = object : HttpClient() {}
}

val httpClientFactory by lazy {
	ServiceLoader.load(HttpClientFactory::class.java).firstOrNull() ?: invalidOp("Can't find implementation for ${HttpClientFactory::class.java.name}")
}

fun createHttpClient() = httpClientFactory.create()