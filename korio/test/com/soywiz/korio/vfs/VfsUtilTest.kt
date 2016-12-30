package com.soywiz.korio.vfs

import org.junit.Assert
import org.junit.Test

class VfsUtilTest {
	@Test
	fun name() {
		Assert.assertEquals("c:/test/hello", VfsUtil.combine("""c:\test\demo""", """..\.\hello"""))
		Assert.assertEquals("d:/lol", VfsUtil.combine("""c:\test\demo""", """d:\lol"""))
	}
}