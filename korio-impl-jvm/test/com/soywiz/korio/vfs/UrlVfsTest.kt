package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

class UrlVfsTest {
	@Test
	fun name() = syncTest {
		Assert.assertEquals("http://test.com/demo/hello/world", UrlVfs("http://test.com/")["demo"].jail()["hello/world"].absolutePath)
		Assert.assertEquals("http://test.com/demo/hello/world", UrlVfs("http://test.com/")["/demo"].jail()["/hello/world"].absolutePath)
	}
}