package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.http.*
import kotlin.test.*

class UniversalVfsTest {
	@Test
	fun testProperVfsIsResolved() {
		assertTrue("file:///path/to/my/file".uniVfs.vfs is LocalVfs)
		assertTrue("http://google.es/".uniVfs.vfs is UrlVfs)
		assertTrue("https://google.es/".uniVfs.vfs is UrlVfs)
	}

	@Test
	fun testProperPathIsResolved() {
		assertEquals("/path/to/my/file", "file:///path/to/my/file".uniVfs.absolutePath)
		assertEquals("http://google.es/", "http://google.es/".uniVfs.absolutePath)
		assertEquals("https://google.es/", "https://google.es/".uniVfs.absolutePath)
	}

	@Test
	fun testProperRequestIsDone() = suspendTest {
		val httpClient = LogHttpClient().apply {
			onRequest().redirect("https://www.google.es/")
			onRequest(url = "https://www.google.es/").response("Worked!")
		}

		assertEquals(
			"Worked!",
			UniversalVfs(
				"https://google.es/",
				defaultUniSchema + UniSchema("https") { UrlVfs(it, httpClient) }
			).readString()
		)

		assertEquals(
			"[GET, https://google.es/, Headers(), null, GET, https://www.google.es/, Headers((Referer, [https://google.es/])), null]",
			httpClient.getAndClearLog().toString()
		)
	}
}
