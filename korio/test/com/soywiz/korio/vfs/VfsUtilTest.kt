package com.soywiz.korio.vfs

import org.junit.Assert
import org.junit.Test

class VfsUtilTest {
	@Test
	fun combine() {
		Assert.assertEquals("c:/test/hello", VfsUtil.combine("""c:\test\demo""", """..\.\hello"""))
		Assert.assertEquals("d:/lol", VfsUtil.combine("""c:\test\demo""", """d:\lol"""))
		Assert.assertEquals("http://hello/world", VfsUtil.combine("""""", """http://hello/world"""))
		Assert.assertEquals("http://hello/demo", VfsUtil.combine("""http://hello/world""", """../demo"""))
		Assert.assertEquals("mailto:demo@demo.com", VfsUtil.combine("""http://hello/world""", """mailto:demo@demo.com"""))
	}

	@Test
	fun isAbsolute() {
		// Absolute
		Assert.assertTrue(VfsUtil.isAbsolute("""C:\.\.\hello"""))
		Assert.assertTrue(VfsUtil.isAbsolute("""/test"""))
		Assert.assertTrue(VfsUtil.isAbsolute("""http://hello"""))
		Assert.assertTrue(VfsUtil.isAbsolute("""ftp://hello"""))
		Assert.assertTrue(VfsUtil.isAbsolute("""mailto:demo@demo.com"""))

		// Relative
		Assert.assertFalse(VfsUtil.isAbsolute("""..\.\hello"""))
		Assert.assertFalse(VfsUtil.isAbsolute("""ftp//hello"""))
		Assert.assertFalse(VfsUtil.isAbsolute("""ftp//hello:world"""))
	}
}