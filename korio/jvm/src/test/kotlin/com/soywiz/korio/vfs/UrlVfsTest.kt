package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.LogHttpClient
import org.junit.Test
import kotlin.test.assertEquals

class UrlVfsTest {
	@Test
	fun name() = syncTest {
		assertEquals("http://test.com/demo/hello/world", UrlVfs("http://test.com/")["demo"].jail()["hello/world"].absolutePath)
		assertEquals("http://test.com/demo/hello/world", UrlVfs("http://test.com/")["/demo"].jail()["/hello/world"].absolutePath)
	}

	@Test
	fun testRightRequests() = syncTest {
		val httpClient = LogHttpClient()
		val url = UrlVfs("http://google.es/", httpClient)
		println(url.readString())
		assertEquals(
			listOf("GET, http://google.es/, Headers(), null"),
			httpClient.log
		)
	}
}