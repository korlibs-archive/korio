package com.soywiz.korio.net.http

import com.soywiz.korio.KorioNative
import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.coroutine.withEventLoop
import com.soywiz.kds.lmapOf
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.*

abstract class HttpClient protected constructor() {
	suspend abstract protected fun requestInternal(method: Http.Method, url: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null): Response

	data class Response(
		val status: Int,
		val statusText: String,
		val headers: Http.Headers,
		val content: AsyncInputStream
	) {
		val success = status < 400
		suspend fun readAllBytes(): ByteArray {
			//println(content)
			val allContent = content.readAll()
			//println("Response.readAllBytes:" + allContent)
			//Debugger.enterDebugger()
			return allContent
		}

		val responseCharset by lazy {
			// @TODO: Detect charset from headers with default to UTF-8
			Charsets.UTF_8
		}

		suspend fun readAllString(charset: Charset = responseCharset): String {
			val bytes = readAllBytes()
			//Debugger.enterDebugger()
			return bytes.toString(charset)
		}

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

	data class RequestConfig(
		val followRedirects: Boolean = true,
		val throwErrors: Boolean = false,
		val maxRedirects: Int = 10,
		val referer: String? = null,
		val simulateBrowser: Boolean = false
	)

	private fun isUrlAbsolute(url: String): Boolean = url.matches(Regex("^\\w+://"))
	private fun mergeUrls(base: String, append: String): String {
		return if (isUrlAbsolute(append)) {
			append
		} else {
			base + "/" + append
		}
	}

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
				val resolvedRedirectLocation = mergeUrls(url, redirectLocation).toString()
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
	suspend fun readJson(url: String, config: RequestConfig = RequestConfig()): Any? = Json.decode(requestAsString(Http.Method.GET, url, config = config.copy(throwErrors = true)).content)

	companion object {
		operator fun invoke() = defaultHttpFactory.createClient()
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
	val CODES = lmapOf(
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

	operator fun invoke(code: Int) = CODES.getOrElse(code) { "Error$code" }
}

object HttpStats {
	val connections = AtomicLong()
	val disconnections = AtomicLong()

	override fun toString(): String = "HttpStats(connections=$connections, Disconnections=$disconnections)"
}

interface HttpFactory {
	fun createClient(): HttpClient
	fun createServer(): HttpServer
}

class ProxiedHttpFactory(var parent: HttpFactory) : HttpFactory by parent

val _defaultHttpFactory: ProxiedHttpFactory by lazy { ProxiedHttpFactory(KorioNative.httpFactory) }
val defaultHttpFactory: HttpFactory get() = _defaultHttpFactory

fun setDefaultHttpFactory(factory: HttpFactory) {
	_defaultHttpFactory.parent = factory
}

fun HttpFactory.createClientEndpoint(endpoint: String) = createClient().endpoint(endpoint)

fun createHttpClient() = defaultHttpFactory.createClient()
fun createHttpServer() = defaultHttpFactory.createServer()
fun createHttpClientEndpoint(endpoint: String) = createHttpClient().endpoint(endpoint)

fun httpError(code: Int, msg: String): Nothing = throw Http.HttpException(code, msg)