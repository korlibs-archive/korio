package com.soywiz.korio.vfs

import org.junit.Assert
import org.junit.Test

class PathInfoTest {
	@Test
	fun name() {
		val PATH1 = "/test/hello.TxT"
		val PATH2 = "C:\\dev\\test\\hello.TxT"
		val PATH3 = "C:\\dev\\test\\hello"

		Assert.assertEquals("/test/hello.TxT", PathInfo(PATH1).fullpath)
		Assert.assertEquals("/test/hello", PathInfo(PATH1).pathWithoutExtension)
		Assert.assertEquals("hello.TxT", PathInfo(PATH1).basename)
		Assert.assertEquals("hello", PathInfo(PATH1).basenameWithoutExtension)
		Assert.assertEquals("TxT", PathInfo(PATH2).extension)
		Assert.assertEquals("txt", PathInfo(PATH2).extensionLC)

		Assert.assertEquals("C:\\dev\\test\\hello.TxT", PathInfo(PATH2).fullpath)
		Assert.assertEquals("C:\\dev\\test\\hello", PathInfo(PATH2).pathWithoutExtension)
		Assert.assertEquals("hello.TxT", PathInfo(PATH2).basename)
		Assert.assertEquals("hello", PathInfo(PATH2).basenameWithoutExtension)
		Assert.assertEquals("TxT", PathInfo(PATH2).extension)
		Assert.assertEquals("txt", PathInfo(PATH2).extensionLC)

		Assert.assertEquals("C:\\dev\\test\\hello", PathInfo(PATH3).fullpath)
		Assert.assertEquals("C:\\dev\\test\\hello", PathInfo(PATH3).pathWithoutExtension)
		Assert.assertEquals("hello", PathInfo(PATH3).basename)
		Assert.assertEquals("hello", PathInfo(PATH3).basenameWithoutExtension)
		Assert.assertEquals("", PathInfo(PATH3).extension)
		Assert.assertEquals("", PathInfo(PATH3).extensionLC)
	}
}