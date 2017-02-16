package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

class VfsFileTest {
	@Test
	fun name() = syncTest {
		val file = MemoryVfs()["C:\\this\\is\\a\\test.txt"]
		Assert.assertEquals("C:/this/is/a", file.parent.fullname)
	}
}