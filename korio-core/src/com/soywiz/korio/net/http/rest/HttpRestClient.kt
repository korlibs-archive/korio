package com.soywiz.korio.net.http.rest

import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.openAsync
import java.io.IOException

class HttpRestClient(val client: HttpClient) {
	suspend fun request(method: Http.Method, url: String, request: Any?): Any {
		val requestContent = request?.let { Json.encode(it) }
		val result = client.request(
			method,
			url,
			content = requestContent?.openAsync(),
			headers = Http.Headers(
				"Content-Type" to "application/json"
			)
		)
		result.checkErrors()
		val stringResult = result.readAllString()
		//println(stringResult)
		return try {
			Json.decode(stringResult) ?: mapOf<String, String>()
		} catch (e: IOException) {
			mapOf<String, String>()
		}
	}

	suspend fun head(url: String): Any = request(Http.Method.HEAD, url, null)
	suspend fun put(url: String, request: Any): Any = request(Http.Method.PUT, url, request)
	suspend fun post(url: String, request: Any): Any = request(Http.Method.POST, url, request)
}

fun HttpClient.rest() = HttpRestClient(this)