package com.soywiz.korio.vfs

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VfsUtilTest {
	@Test
	fun combine() {
		assertEquals("c:/test/hello", VfsUtil.combine("""c:\test\demo""", """..\.\hello"""))
		assertEquals("d:/lol", VfsUtil.combine("""c:\test\demo""", """d:\lol"""))
		assertEquals("http://hello/world", VfsUtil.combine("""""", """http://hello/world"""))
		assertEquals("http://hello/demo", VfsUtil.combine("""http://hello/world""", """../demo"""))
		assertEquals("mailto:demo@demo.com", VfsUtil.combine("""http://hello/world""", """mailto:demo@demo.com"""))
	}

	@Test
	fun isAbsolute() {
		// Absolute
		assertTrue(VfsUtil.isAbsolute("""C:\.\.\hello"""))
		assertTrue(VfsUtil.isAbsolute("""/test"""))
		assertTrue(VfsUtil.isAbsolute("""http://hello"""))
		assertTrue(VfsUtil.isAbsolute("""ftp://hello"""))
		assertTrue(VfsUtil.isAbsolute("""mailto:demo@demo.com"""))

		// Relative
		assertFalse(VfsUtil.isAbsolute("""..\.\hello"""))
		assertFalse(VfsUtil.isAbsolute("""ftp//hello"""))
		assertFalse(VfsUtil.isAbsolute("""ftp//hello:world"""))
	}
}