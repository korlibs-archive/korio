package com.soywiz.korio.net

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.LogHttpClient
import org.junit.Test
import kotlin.test.assertEquals

class HttpClientTest {
	@Test
	fun testFullRedirections() = syncTest {
		val httpClient = LogHttpClient().apply {
			onRequest().redirect("https://www.google.es/")
			onRequest(url = "https://www.google.es/").ok("Worked!")
		}

		assertEquals("Worked!", httpClient.request(Http.Method.GET, "https://google.es/").readAllString())
		assertEquals(
			"[GET, https://google.es/, Headers(), null, GET, https://www.google.es/, Headers((Referer, [https://google.es/])), null]",
			httpClient.getAndClearLog().toString()
		)
	}
}