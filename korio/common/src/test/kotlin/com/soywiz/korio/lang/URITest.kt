package com.soywiz.korio.lang

import org.junit.Test
import kotlin.test.assertEquals

class URITest {
	data class UriInfo(val uri: String, val componentString: String, val isAbsolute: Boolean, val isOpaque: Boolean)

	val URIS = listOf(
		UriInfo("", componentString = "URI(path=)", isAbsolute = false, isOpaque = false),
		UriInfo("hello", componentString = "URI(path=hello)", isAbsolute = false, isOpaque = false),
		UriInfo("/hello", componentString = "URI(path=/hello)", isAbsolute = false, isOpaque = false),
		UriInfo("/hello?world", componentString = "URI(path=/hello, query=world)", isAbsolute = false, isOpaque = false),
		UriInfo("/hello?world?world", componentString = "URI(path=/hello, query=world?world)", isAbsolute = false, isOpaque = false),
		UriInfo("http://", componentString = "URI(scheme=http, path=)", isAbsolute = true, isOpaque = false),
		UriInfo("http://hello", componentString = "URI(scheme=http, host=hello, path=)", isAbsolute = true, isOpaque = false),
		UriInfo("http://user:pass@hello", componentString = "URI(scheme=http, userInfo=user:pass, host=hello, path=)", isAbsolute = true, isOpaque = false),
		UriInfo("http://user:pass@hello/path", componentString = "URI(scheme=http, userInfo=user:pass, host=hello, path=/path)", isAbsolute = true, isOpaque = false),
		UriInfo("http://user:pass@hello/path?query", componentString = "URI(scheme=http, userInfo=user:pass, host=hello, path=/path, query=query)", isAbsolute = true, isOpaque = false),
		UriInfo("http://hello/path", componentString = "URI(scheme=http, host=hello, path=/path)", isAbsolute = true, isOpaque = false),
		UriInfo("http://hello?query", componentString = "URI(scheme=http, host=hello, path=, query=query)", isAbsolute = true, isOpaque = false),
		UriInfo("mailto:demo@host.com", componentString = "URI(scheme=mailto, userInfo=demo, host=host.com, path=)", isAbsolute = true, isOpaque = true)
	)

	@Test
	fun testParsing() {
		for (uri in URIS) assertEquals(uri.componentString, URI(uri.uri).toComponentString(), uri.uri)
	}

	@Test
	fun testFullUrl() {
		for (uri in URIS) assertEquals(uri.uri, URI(uri.uri).fullUri, uri.uri)
	}

	@Test
	fun testIsAbsolute() {
		for (uri in URIS) assertEquals(uri.isAbsolute, URI(uri.uri).isAbsolute, uri.uri)
	}

	@Test
	fun testIsOpaque() {
		for (uri in URIS) assertEquals(uri.isOpaque, URI(uri.uri).isOpaque, uri.uri)
	}
}
