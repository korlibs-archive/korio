package com.soywiz.korio.net.http

import com.jtransc.cs.CSharp
import com.soywiz.korio.stream.AsyncStream

class HttpFactoryCSharp : HttpFactory() {
	override fun createClient(): HttpClient = CSharpHttpClient()
}

class CSharpHttpClient : HttpClient() {
	private val csClient = CSharp.raw<Any>("N.wrap(new System.Net.Http.HttpClient())")

	private fun createCSharpHttpMethod(kind: String) = CSharp.raw<String>("N.str(new System.Net.Http.HttpMethod(N.istr(p0)))")

	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		// System.Net.Http.HttpClient
		// OpenReadAsync
		// OpenWriteAsync
		TODO()
	}
}