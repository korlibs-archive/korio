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

	@Test
	fun redirector() = syncTest {
		val file = MemoryVfsMix(
			"hello.txt" to "yay!",
			"hello.bin" to "NEVER-HERE"
		).redirect { PathInfo(it).pathWithExtension("txt") }

		Assert.assertEquals("yay!", file["hello.txt"].readString())
		Assert.assertEquals("yay!", file["hello.bin"].readString())
	}

}