package com.soywiz.korio.net.http

import com.soywiz.korio.async.Promise
import com.soywiz.korio.ds.MapList
import com.soywiz.korio.service.Services
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.Cancellable
import java.io.IOException
import java.nio.charset.Charset

interface Http {
	enum class Method {
		//HEAD, POST, GET, PUT, DELETE, OPTIONS, TRACE
		OPTIONS,
		GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT, PATCH, OTHER
	}

	data class Headers(val items: MapList<String, String> = MapList()) : Iterable<Pair<String, String>> {
		val itemsCI = MapList<String, String>(items.toList().flatMap { (key, values) -> values.map { key.toLowerCase() to it } })

		//class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>
		override fun iterator(): Iterator<Pair<String, String>> = items.flatMapIterator()

		constructor(map: Map<String, String>) : this(MapList(map.entries.map { it.key to it.value }))
		constructor(str: String?) : this(parse(str))
		constructor(vararg items: Pair<String, String>) : this(MapList(items.toList()))

		operator fun get(key: String): String? = itemsCI.getFirst(key.toLowerCase())

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
}

open class HttpClient protected constructor() {
	data class Response(
			val status: Int,
			val statusText: String,
			val headers: Http.Headers,
			val content: AsyncInputStream
	) {
		val success = status < 400
		suspend fun readAllBytes() = content.readAll()

		fun withStringResponse(str: String, charset: Charset = Charsets.UTF_8) = this.copy(content = str.toByteArray(charset).openAsync())
	}

	suspend open fun request(method: Http.Method, url: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null): Response {
		TODO()
	}

	class HttpException(val msg: String) : IOException(msg)

	suspend fun readBytes(url: String): ByteArray {
		val res = request(Http.Method.GET, url)
		if (!res.success) throw HttpException("Http error: " + res.status + " " + res.statusText)
		return res.content.readAll()
	}

	suspend fun readString(url: String, charset: Charset = Charsets.UTF_8): String {
		return readBytes(url).toString(charset)
	}

	companion object {
		operator fun invoke() = httpFactory.createClient()
	}
}

open class HttpServer protected constructor() {
	companion object {
		operator fun invoke() = httpFactory.createServer()
	}

	class Request {
	}

	suspend open fun listen(port: Int, host: String = "127.0.0.1", handler: suspend (Request) -> Unit) {
		val deferred = Promise.Deferred<Unit>()
		deferred.onCancel {

		}
		deferred.promise.await()
	}
}

class LogHttpClient : HttpClient() {
	val log = arrayListOf<String>()
	var response = HttpClient.Response(200, "OK", Http.Headers(), "LogHttpClient.response".toByteArray(Charsets.UTF_8).openAsync())

	suspend override fun request(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		val contentString = content?.readAll()?.toString(Charsets.UTF_8)
		log += "$method, $url, $headers, $contentString"
		return response
	}

	fun getAndClearLog() = log.toList().apply { log.clear() }
}

open class HttpFactory : Services.Impl() {
	open fun createClient(): HttpClient = object : HttpClient() {}
	open fun createServer(): HttpServer = object : HttpServer() {}
}

val httpFactory by lazy { Services.load<HttpFactory>() }

fun createHttpClient() = httpFactory.createClient()
fun createHttpServer() = httpFactory.createServer()