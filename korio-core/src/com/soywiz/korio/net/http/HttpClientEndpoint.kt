package com.soywiz.korio.net.http

import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import java.net.URI

interface HttpClientEndpoint {
	suspend fun request(method: Http.Method, path: String, headers: Http.Headers = Http.Headers(), content: AsyncStream? = null, config: HttpClient.RequestConfig = HttpClient.RequestConfig()): HttpClient.Response
}

class FakeHttpClientEndpoint(val defaultMessage: String = "{}") : HttpClientEndpoint {
	data class Request(val method: Http.Method, val path: String, val headers: Http.Headers, val content: AsyncStream?, val config: HttpClient.RequestConfig)

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
		log += Request(method, path, headers, content, config)
		if (responses.isEmpty()) addOkResponse(defaultMessage)
		return responses.getOrElse(responsePointer++ % responses.size) {
			getResponse(200, defaultMessage)
		}
	}

	val FORMAT_REGEX = Regex("\\{\\w+\\}")

	suspend fun capture(format: String = "{METHOD}:{PATH}:{CONTENT}", callback: suspend () -> Unit): List<String> {
		val start = log.size
		callback()
		val end = log.size
		return log.slice(start until end).map { request ->
			val content = request.content?.readAll()?.toString(Charsets.UTF_8)
			format.replace(FORMAT_REGEX) {
				val name = it.groupValues[0]
				when (name) {
					"{METHOD}" -> "${request.method}"
					"{PATH}" -> request.path
					"{CONTENT}" -> "$content"
					else -> name
				}
			}
		}
	}
}

fun HttpClient.endpoint(endpoint: String): HttpClientEndpoint {
	val client = this
	val rendpoint = URI(endpoint)
	return object : HttpClientEndpoint {
		override suspend fun request(method: Http.Method, path: String, headers: Http.Headers, content: AsyncStream?, config: HttpClient.RequestConfig): HttpClient.Response {
			val resolvedUrl = rendpoint.resolve("/" + path.trimStart('/')).toString()
			return client.request(method, resolvedUrl, headers, content, config)
		}
	}
}