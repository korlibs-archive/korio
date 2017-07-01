package com.soywiz.korio.net.http

import com.soywiz.korio.stream.AsyncStream

class HttpFactoryAs3 : HttpFactory() {
	override fun createClient(): HttpClient = HttpClientAs3()
}

class HttpClientAs3 : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		return super.requestInternal(method, url, headers, content)
	}
}