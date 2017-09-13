package com.soywiz.korio.net.http

import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.async.Promise
import com.soywiz.korio.coroutine.withEventLoop
import com.soywiz.korio.crypto.fromBase64
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.service.Services
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.AsyncCloseable
import java.io.IOException
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicLong

interface Http {
	data class Method(val _name: String) {
		val nameUC = _name.trim().toUpperCase()
		val name get() = nameUC

		companion object {
			@JvmStatic
			val OPTIONS = Method("OPTIONS")
			@JvmStatic
			val GET = Method("GET")
			@JvmStatic
			val HEAD = Method("HEAD")
			@JvmStatic
			val POST = Method("POST")
			@JvmStatic
			val PUT = Method("PUT")
			@JvmStatic
			val DELETE = Method("DELETE")
			@JvmStatic
			val TRACE = Method("TRACE")
			@JvmStatic
			val CONNECT = Method("CONNECT")
			@JvmStatic
			val PATCH = Method("PATCH")

			val values = listOf(OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT, PATCH)
			fun values() = values
		}

		override fun toString(): String = nameUC
	}

	class HttpException(
		val statusCode: Int,
		val msg: String = "Error$statusCode",
		val statusText: String = HttpStatusMessage.CODES[statusCode] ?: "Error$statusCode",
		val headers: Http.Headers = Http.Headers()
	) : IOException("$statusCode $statusText - $msg") {
		companion object {
			fun unauthorizedBasic(realm: String = "Realm", msg: String = "Unauthorized"): Nothing = throw Http.HttpException(401, msg = msg, headers = Http.Headers("WWW-Authenticate" to "Basic realm=\"$realm\""))
			//fun unauthorizedDigest(realm: String = "My Domain", msg: String = "Unauthorized"): Nothing = throw Http.HttpException(401, msg = msg, headers = Http.Headers("WWW-Authenticate" to "Digest realm=\"$realm\""))
		}
	}

	data class Auth(
		val user: String,
		val pass: String,
		val digest: String
	) {
		companion object {
			fun parse(auth: String): Auth {
				val parts = auth.split(' ', limit = 2)
				if (parts[0].equals("basic", ignoreCase = true)) {
					val parts = parts[1].fromBase64().toString(Charsets.UTF_8).split(':', limit = 2)
					return Auth(user = parts[0], pass = parts[1], digest = "")
				} else if (parts[0].isEmpty()) {
					return Auth(user = "", pass = "", digest = "")
				} else {
					invalidOp("Just supported basic auth")
				}
			}
		}

		fun validate(expectedUser: String, expectedPass: String, realm: String = "Realm"): Boolean {
			if (this.user == expectedUser && this.pass == expectedPass) return true
			return false
		}

		suspend fun checkBasic(realm: String = "Realm", check: suspend Auth.() -> Boolean) {
			if (user.isEmpty() || !check(this)) Http.HttpException.unauthorizedBasic(realm = "Domain", msg = "Invalid auth")
		}
	}

	class Response {
		val headers = arrayListOf<Pair<String, String>>()

		fun header(key: String, value: String) {
			headers += key to value
		}
	}

	data class Headers(val items: List<Pair<String, String>>) : Iterable<Pair<String, String>> {
		constructor(vararg items: Pair<String, String>) : this(items.toList())
		constructor(map: Map<String, String>) : this(map.map { it.key to it.value })
		constructor(str: String?) : this(parse(str).items)

		override fun iterator(): Iterator<Pair<String, String>> = items.iterator()

		operator fun get(key: String): String? = getFirst(key)
		fun getAll(key: String): List<String> = items.filter { it.first.equals(key, ignoreCase = true) }.map { it.second }
		fun getFirst(key: String): String? = items.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second

		fun toListGrouped(): List<Pair<String, List<String>>> {
			return this.items.groupBy { it.first.toLowerCase() }.map { it.value.first().first to it.value.map { it.second } }.sortedBy { it.first.toLowerCase() }
		}

		fun withAppendedHeaders(newHeaders: List<Pair<String, String>>): Headers = Headers(this.items + newHeaders.toList())
		fun withReplaceHeaders(newHeaders: List<Pair<String, String>>): Headers {
			val replaceKeys = newHeaders.map { it.first.toLowerCase() }.toSet()

			return Headers(this.items.filter { it.first.toLowerCase() !in replaceKeys } + newHeaders.toList())
		}

		fun withAppendedHeaders(vararg newHeaders: Pair<String, String>): Headers = withAppendedHeaders(newHeaders.toList())
		fun withReplaceHeaders(vararg newHeaders: Pair<String, String>): Headers = withReplaceHeaders(newHeaders.toList())

		operator fun plus(that: Headers): Headers = withAppendedHeaders(this.items + that.items)

		override fun toString(): String = "Headers(${toListGrouped().joinToString(", ")})"

		companion object {
			fun fromListMap(map: Map<String?, List<String>>): Headers {
				return Headers(map.flatMap { pair -> if (pair.key == null) listOf() else pair.value.map { value -> pair.key!! to value } })
			}

			fun parse(str: String?): Headers {
				if (str == null) return Headers()
				return Headers(str.split("\n").map {
					val parts = it.trim().split(':', limit = 2)
					if (parts.size >= 2) parts[0].trim() to parts[1].trim() else null
				}.filterNotNull())
			}
		}
	}

	/*
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

		operator fun plus(that: Headers): Headers {
			return Headers(*that.items, )
		}

		override fun toString(): String = "Headers(${items.joinToString(", ")})"
	}
	*/
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

		val responseCharset by lazy {
			// @TODO: Detect charset from headers with default to UTF-8
			Charsets.UTF_8
		}

		suspend fun readAllString(charset: Charset = responseCharset) = readAllBytes().toString(charset)

		suspend fun checkErrors(): Response = this.apply {
			if (!success) throw Http.HttpException(status, readAllString(), statusText)
		}

		fun withStringResponse(str: String, charset: Charset = Charsets.UTF_8) = this.copy(content = str.toByteArray(charset).openAsync())

		fun <T> toCompletedResponse(content: T) = CompletedResponse(status, statusText, headers, content)
	}

	data class CompletedResponse<T>(
		val status: Int,
		val statusText: String,
		val headers: Http.Headers,
		val content: T
	) {
		val success = status < 400
	}

	suspend open protected fun requestInternal(method: Http.Method, url: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null): Response {
		TODO()
	}

	data class RequestConfig(
		val followRedirects: Boolean = true,
		val throwErrors: Boolean = false,
		val maxRedirects: Int = 10,
		val referer: String? = null,
		val simulateBrowser: Boolean = false
	)

	suspend fun request(method: Http.Method, url: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null, config: RequestConfig = RequestConfig()): Response {
		val contentLength = content?.getLength() ?: 0L
		var actualHeaders = headers

		if (content != null && !headers.any { it.first.equals("content-length", ignoreCase = true) }) {
			actualHeaders = actualHeaders.withReplaceHeaders("content-length" to "$contentLength")
		}

		if (config.simulateBrowser) {
			if (actualHeaders["user-agent"] == null) {
				actualHeaders = actualHeaders.withReplaceHeaders(
					"Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
					"user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36"
				)
			}
		}

		//println("$method: $url ($config)")
		//for (header in actualHeaders) println(" ${header.first}: ${header.second}")

		val response = requestInternal(method, url, actualHeaders, content).apply { if (config.throwErrors) checkErrors() }
		if (config.followRedirects && config.maxRedirects >= 0) {
			val redirectLocation = response.headers["location"]
			if (redirectLocation != null) {
				//for (header in response.headers) println(header)
				//println("Method: $method")
				//println("Location: $location")
				val resolvedRedirectLocation = URI(url).resolve(redirectLocation).toString()
				//println("Redirect: $redirectLocation")
				//println("Redirect: ${URI(url).resolve(redirectLocation)}")

				return request(method, resolvedRedirectLocation, headers.withReplaceHeaders(
					"Referer" to url
				), content, config.copy(maxRedirects = config.maxRedirects - 1))
			}
		}
		return response
	}

	suspend fun requestAsString(method: Http.Method, url: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null, config: RequestConfig = RequestConfig()): CompletedResponse<String> {
		val res = request(method, url, headers, content, config = config)
		return res.toCompletedResponse(res.readAllString())
	}

	suspend fun requestAsBytes(method: Http.Method, url: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null, config: RequestConfig = RequestConfig()): CompletedResponse<ByteArray> {
		val res = request(method, url, headers, content, config = config)
		return res.toCompletedResponse(res.readAllBytes())
	}

	suspend fun readBytes(url: String, config: RequestConfig = RequestConfig()): ByteArray = requestAsBytes(Http.Method.GET, url, config = config.copy(throwErrors = true)).content
	suspend fun readString(url: String, config: RequestConfig = RequestConfig()): String = requestAsString(Http.Method.GET, url, config = config.copy(throwErrors = true)).content

	companion object {
		operator fun invoke() = httpFactory.createClient()
	}
}

open class DelayedHttpClient(val delayMs: Int, val parent: HttpClient) : HttpClient() {
	private val queue = AsyncThread()

	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = queue {
		withEventLoop {
			println("Waiting $delayMs milliseconds for $url...")
			sleep(delayMs)
			parent.request(method, url, headers, content)
		}
	}
}

fun HttpClient.delayed(ms: Int) = DelayedHttpClient(ms, this)

open class HttpServer protected constructor() : AsyncCloseable {
	companion object {
		operator fun invoke() = httpFactory.createServer()
	}

	class Request {
	}

	suspend open protected fun listenInternal(port: Int, host: String = "127.0.0.1", handler: suspend (Request) -> Unit) {
		val deferred = Promise.Deferred<Unit>()
		deferred.onCancel {

		}
		deferred.promise.await()
	}

	open val actualPort: Int = 0

	suspend open protected fun closeInternal() {
	}

	suspend fun listen(port: Int, host: String = "127.0.0.1", handler: suspend (Request) -> Unit): HttpServer {
		listenInternal(port, host, handler)
		return this
	}

	suspend override fun close() {
		closeInternal()
	}
}

class LogHttpClient(val redirect: HttpClient? = null) : HttpClient() {
	val log = arrayListOf<String>()
	var response = HttpClient.Response(200, "OK", Http.Headers(), "LogHttpClient.response".toByteArray(Charsets.UTF_8).openAsync())

	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		val contentString = content?.slice()?.readAll()?.toString(Charsets.UTF_8)
		log += "$method, $url, $headers, $contentString"
		if (redirect != null) {
			return redirect.request(method, url, headers, content)
		} else {
			return response
		}
	}

	fun setTextResponse(text: String, status: Int = 200, statusText: String = "OK", headers: Http.Headers = Http.Headers()): LogHttpClient {
		response = HttpClient.Response(status, statusText, headers, text.toByteArray(Charsets.UTF_8).openAsync())
		return this
	}

	fun getAndClearLog() = log.toList().apply { log.clear() }
}

object HttpStatusMessage {
	val CODES = mapOf(
		100 to "Continue",
		101 to "Switching Protocols",
		200 to "OK",
		201 to "Created",
		202 to "Accepted",
		203 to "Non-Authoritative Information",
		204 to "No Content",
		205 to "Reset Content",
		206 to "Partial Content",
		300 to "Multiple Choices",
		301 to "Moved Permanently",
		302 to "Found",
		303 to "See Other",
		304 to "Not Modified",
		305 to "Use Proxy",
		307 to "Temporary Redirect",
		400 to "Bad Request",
		401 to "Unauthorized",
		402 to "Payment Required",
		403 to "Forbidden",
		404 to "Not Found",
		405 to "Method Not Allowed",
		406 to "Not Acceptable",
		407 to "Proxy Authentication Required",
		408 to "Request Timeout",
		409 to "Conflict",
		410 to "Gone",
		411 to "Length Required",
		412 to "Precondition Failed",
		413 to "Request Entity Too Large",
		414 to "Request-URI Too Long",
		415 to "Unsupported Media Type",
		416 to "Requested Range Not Satisfiable",
		417 to "Expectation Failed",
		418 to "I'm a teapot",
		422 to "Unprocessable Entity (WebDAV - RFC 4918)",
		423 to "Locked (WebDAV - RFC 4918)",
		424 to "Failed Dependency (WebDAV) (RFC 4918)",
		425 to "Unassigned",
		426 to "Upgrade Required (RFC 7231)",
		428 to "Precondition Required",
		429 to "Too Many Requests",
		431 to "Request Header Fileds Too Large)",
		449 to "Error449",
		451 to "Unavailable for Legal Reasons",
		500 to "Internal Server Error",
		501 to "Not Implemented",
		502 to "Bad Gateway",
		503 to "Service Unavailable",
		504 to "Gateway Timeout",
		505 to "HTTP Version Not Supported",
		506 to "Variant Also Negotiates (RFC 2295)",
		507 to "Insufficient Storage (WebDAV - RFC 4918)",
		508 to "Loop Detected (WebDAV)",
		509 to "Bandwidth Limit Exceeded",
		510 to "Not Extended (RFC 2774)",
		511 to "Network Authentication Required"
	)

}

object HttpStats {
	val connections = AtomicLong()
	val disconnections = AtomicLong()

	override fun toString(): String = "HttpStats(connections=$connections, Disconnections=$disconnections)"
}

open class HttpFactory : Services.Impl() {
	open fun createClient(): HttpClient = object : HttpClient() {}
	open fun createServer(): HttpServer = object : HttpServer() {}
}

val defaultHttpFactory by lazy { Services.load<HttpFactory>() }

@Deprecated("Use defaultHttpFactory instead", ReplaceWith("defaultHttpFactory"))
val httpFactory
	get() = defaultHttpFactory

fun createHttpClient() = defaultHttpFactory.createClient()
fun createHttpServer() = defaultHttpFactory.createServer()

fun httpError(code: Int, msg: String): Nothing = throw Http.HttpException(code, msg)