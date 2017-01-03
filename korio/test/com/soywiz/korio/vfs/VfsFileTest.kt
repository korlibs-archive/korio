package com.soywiz.korio.vfs

import com.soywiz.korio.async.sync
import org.junit.Assert
import org.junit.Test

class VfsFileTest {
	@Test
	fun name() = sync {
		val file = MemoryVfs()["C:\\this\\is\\a\\test.txt"]
		Assert.assertEquals("C:/this/is/a", file.parent.fullname)
	}
}