package com.soywiz.korio.net.http.rest

import com.soywiz.korio.lang.IOException
import com.soywiz.korio.net.http.*
import com.soywiz.korio.serialization.ObjectMapper
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.openAsync

class HttpRestClient(val endpoint: HttpClientEndpoint, val mapper: ObjectMapper) {
	suspend fun request(method: Http.Method, path: String, request: Any?): Any {
		val requestContent = request?.let { Json.encode(it, mapper) }
		val result = endpoint.request(
			method,
			path,
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

	suspend fun head(path: String): Any = request(Http.Method.HEAD, path, null)
	suspend fun delete(path: String): Any = request(Http.Method.DELETE, path, null)
	suspend fun get(path: String): Any = request(Http.Method.GET, path, null)
	suspend fun put(path: String, request: Any): Any = request(Http.Method.PUT, path, request)
	suspend fun post(path: String, request: Any): Any = request(Http.Method.POST, path, request)
}

fun HttpClientEndpoint.rest(mapper: ObjectMapper) = HttpRestClient(this, mapper)
fun HttpClient.rest(endpoint: String, mapper: ObjectMapper) = HttpRestClient(this.endpoint(endpoint), mapper)
fun HttpFactory.createRestClient(endpoint: String, mapper: ObjectMapper) = createClient().endpoint(endpoint).rest(mapper)
