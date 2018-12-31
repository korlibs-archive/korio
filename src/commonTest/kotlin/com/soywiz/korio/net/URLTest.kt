package com.soywiz.korio.net

import kotlin.test.*

class URLTest {
	data class UriInfo(val uri: String, val componentString: String, val isAbsolute: Boolean, val isOpaque: Boolean)

	val URIS = listOf(
		UriInfo("", componentString = "URI(path=)", isAbsolute = false, isOpaque = false),
		UriInfo("hello", componentString = "URI(path=hello)", isAbsolute = false, isOpaque = false),
		UriInfo("/hello", componentString = "URI(path=/hello)", isAbsolute = false, isOpaque = false),
		UriInfo(
			"/hello?world",
			componentString = "URI(path=/hello, query=world)",
			isAbsolute = false,
			isOpaque = false
		),
		UriInfo(
			"/hello?world?world",
			componentString = "URI(path=/hello, query=world?world)",
			isAbsolute = false,
			isOpaque = false
		),
		UriInfo("http://", componentString = "URI(scheme=http, path=)", isAbsolute = true, isOpaque = false),
		UriInfo(
			"http://hello",
			componentString = "URI(scheme=http, host=hello, path=)",
			isAbsolute = true,
			isOpaque = false
		),
		UriInfo(
			"http://hello/",
			componentString = "URI(scheme=http, host=hello, path=/)",
			isAbsolute = true,
			isOpaque = false
		),
		UriInfo(
			"http://user:pass@hello",
			componentString = "URI(scheme=http, userInfo=user:pass, host=hello, path=)",
			isAbsolute = true,
			isOpaque = false
		),
		UriInfo(
			"http://user:pass@hello/path",
			componentString = "URI(scheme=http, userInfo=user:pass, host=hello, path=/path)",
			isAbsolute = true,
			isOpaque = false
		),
		UriInfo(
			"http://user:pass@hello/path?query",
			componentString = "URI(scheme=http, userInfo=user:pass, host=hello, path=/path, query=query)",
			isAbsolute = true,
			isOpaque = false
		),
		UriInfo(
			"http://hello/path",
			componentString = "URI(scheme=http, host=hello, path=/path)",
			isAbsolute = true,
			isOpaque = false
		),
		UriInfo(
			"http://hello?query",
			componentString = "URI(scheme=http, host=hello, path=, query=query)",
			isAbsolute = true,
			isOpaque = false
		),
		UriInfo(
			"mailto:demo@host.com",
			componentString = "URI(scheme=mailto, userInfo=demo, host=host.com, path=)",
			isAbsolute = true,
			isOpaque = true
		),
		UriInfo(
			"http://hello?query#hash",
			componentString = "URI(scheme=http, host=hello, path=, query=query, fragment=hash)",
			isAbsolute = true,
			isOpaque = false
		)
	)

	@Test
	fun testParsing() {
		for (uri in URIS) assertEquals(uri.componentString, URL(uri.uri).toComponentString(), uri.uri)
	}

	@Test
	fun testFullUrl() {
		for (uri in URIS) assertEquals(uri.uri, URL(uri.uri).fullUri, uri.uri)
	}

	@Test
	fun testIsAbsolute() {
		for (uri in URIS) assertEquals(uri.isAbsolute, URL(uri.uri).isAbsolute, uri.uri)
	}

	@Test
	fun testIsOpaque() {
		for (uri in URIS) assertEquals(uri.isOpaque, URL(uri.uri).isOpaque, uri.uri)
	}

	@Test
	fun testResolve() {
		assertEquals("https://www.google.es/", URL.resolve("https://google.es/", "https://www.google.es/"))
		assertEquals("https://google.es/demo", URL.resolve("https://google.es/path", "demo"))
		assertEquals("https://google.es/path/demo", URL.resolve("https://google.es/path/", "demo"))
		assertEquals("https://google.es/demo", URL.resolve("https://google.es/path/", "/demo"))
		assertEquals("https://google.es/test", URL.resolve("https://google.es/path/path2", "../test"))
		assertEquals("https://google.es/path/test", URL.resolve("https://google.es/path/path2/", "../test"))
		assertEquals("https://google.es/test", URL.resolve("https://google.es/path/path2/", "../../../test"))
	}

	@Test
	fun testEncode() {
		assertEquals("hello%20world", URL.encodeComponent("hello world"))
		assertEquals("hello+world", URL.encodeComponent("hello world", formUrlEncoded = true))

		assertEquals("hello%2Bworld", URL.encodeComponent("hello+world"))
		assertEquals("hello%2Bworld", URL.encodeComponent("hello+world", formUrlEncoded = true))

		assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789%20-_.*", URL.encodeComponent("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789 -_.*"))
		assertEquals("%C3%A1%C3%A9%C3%AD%C3%B3%C3%BA", URL.encodeComponent("áéíóú"))
	}

	@Test
	fun testDecode() {
		assertEquals("hello world", URL.decodeComponent("hello%20world"))
		assertEquals("hello+world+", URL.decodeComponent("hello+world%2B"))
		assertEquals("hello world+", URL.decodeComponent("hello+world%2B", formUrlEncoded = true))
		assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789 -_.*", URL.decodeComponent("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWZ0123456789%20-_.*"))
		assertEquals("áéíóú", URL.decodeComponent("%C3%A1%C3%A9%C3%AD%C3%B3%C3%BA"))
	}
}
