package com.soywiz.korio.net.http

import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.URIUtils
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll

interface HttpClientEndpoint {
	suspend fun request(method: Http.Method, path: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null, config: HttpClient.RequestConfig = HttpClient.RequestConfig()): HttpClient.Response
}

data internal class Request(val method: Http.Method, val path: String, val headers: Http.Headers, val content: AsyncStream?) {
	companion object {
		val FORMAT_REGEX = Regex("\\{\\w+\\}")
	}

	suspend fun format(format: String = "{METHOD}:{PATH}:{CONTENT}"): String {
		val content = content?.readAll()?.toString(Charsets.UTF_8)
		return format.replace(FORMAT_REGEX) {
			val name = it.groupValues[0]
			when (name) {
				"{METHOD}" -> "$method"
				"{PATH}" -> path
				"{CONTENT}" -> "$content"
				else -> name
			}
		}
	}
}

class FakeHttpClient(val defaultMessage: String = "{}") : HttpClient() {
	private val log = arrayListOf<Request>()
	private var responsePointer = 0
	private val responses = arrayListOf<HttpClient.Response>()

	private fun getResponse(code: Int, content: String) = HttpClient.Response(code, HttpStatusMessage.CODES[code] ?: "Code$code", Http.Headers(), content.openAsync())

	fun addResponse(code: Int, content: String) {
		responses += getResponse(code, content)
	}

	fun addOkResponse(content: String) = addResponse(200, content)
	fun addNotFoundResponse(content: String) = addResponse(404, content)

	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		log += Request(method, url, headers, content)
		if (responses.isEmpty()) addOkResponse(defaultMessage)
		return responses.getOrElse(responsePointer++ % responses.size) {
			getResponse(200, defaultMessage)
		}
	}

	suspend fun capture(format: String = "{METHOD}:{PATH}:{CONTENT}", callback: suspend () -> Unit): List<String> {
		val start = log.size
		callback()
		val end = log.size
		return log.slice(start until end).map { it.format(format) }
	}
}

class FakeHttpClientEndpoint(val defaultMessage: String = "{}") : HttpClientEndpoint {
	private val log = arrayListOf<Request>()
	private var responsePointer = 0
	private val responses = arrayListOf<HttpClient.Response>()

	private fun getResponse(code: Int, content: String) = HttpClient.Response(code, HttpStatusMessage.CODES[code] ?: "Code$code", Http.Headers(), content.openAsync())

	fun addResponse(code: Int, content: String) {
		responses += getResponse(code, content)
	}

	fun addOkResponse(content: String) = addResponse(200, content)
	fun addNotFoundResponse(content: String) = addResponse(404, content)

	suspend override fun request(method: Http.Method, path: String, headers: Http.Headers, content: AsyncStream?, config: HttpClient.RequestConfig): HttpClient.Response {
		log += Request(method, path, headers, content)
		if (responses.isEmpty()) addOkResponse(defaultMessage)
		return responses.getOrElse(responsePointer++ % responses.size) {
			getResponse(200, defaultMessage)
		}
	}

	suspend fun capture(format: String = "{METHOD}:{PATH}:{CONTENT}", callback: suspend () -> Unit): List<String> {
		val start = log.size
		callback()
		val end = log.size
		return log.slice(start until end).map { it.format(format) }
	}
}

fun HttpClient.endpoint(endpoint: String): HttpClientEndpoint {
	val client = this
	return object : HttpClientEndpoint {
		override suspend fun request(method: Http.Method, path: String, headers: Http.Headers, content: AsyncStream?, config: HttpClient.RequestConfig): HttpClient.Response {
			val resolvedUrl = URIUtils.resolve(endpoint, "/" + path.trimStart('/')).toString()
			return client.request(method, resolvedUrl, headers, content, config)
		}
	}
}