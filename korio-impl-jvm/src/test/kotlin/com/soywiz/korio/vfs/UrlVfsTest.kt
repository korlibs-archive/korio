package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import org.junit.Test
import kotlin.test.assertEquals

class UrlVfsTest {
	@Test
	fun name() = syncTest {
		assertEquals("http://test.com/demo/hello/world", UrlVfs("http://test.com/")["demo"].jail()["hello/world"].absolutePath)
		assertEquals("http://test.com/demo/hello/world", UrlVfs("http://test.com/")["/demo"].jail()["/hello/world"].absolutePath)
	}
}