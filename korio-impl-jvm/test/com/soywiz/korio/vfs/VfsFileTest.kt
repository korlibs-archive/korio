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
	fun memoryNonExists() = syncTest {
		val file = MemoryVfs()
		Assert.assertEquals(false, file["test"].exists())
	}

	@Test
	fun testCaseSensitiveAccess() = syncTest {
		val file = MemoryVfs(caseSensitive = true)
		file["test.tXt"].writeString("hello world")
		Assert.assertEquals(true, file["test.tXt"].exists())
		Assert.assertEquals(false, file["test.txt"].exists())
		Assert.assertEquals(false, file["test.TXT"].exists())
	}

	@Test
	fun testCaseInsensitiveAccess() = syncTest {
		val file = MemoryVfs(caseSensitive = false)
		file["test.tXt"].writeString("hello world")
		Assert.assertEquals(true, file["test.txt"].exists())
		Assert.assertEquals(true, file["test.tXt"].exists())
		Assert.assertEquals(true, file["test.TXT"].exists())
	}

	@Test
	fun redirected() = syncTest {
		var out = ""
		val file = MemoryVfsMix(
			"hello.txt" to "yay!",
			"hello.bin" to "NEVER-HERE"
		).redirected {
			out += this[it].readString()
			PathInfo(it).pathWithExtension("txt")
		}

		Assert.assertEquals("yay!", file["hello.txt"].readString())
		Assert.assertEquals("yay!", out)
		Assert.assertEquals("yay!", file["hello.bin"].readString())
		Assert.assertEquals("yay!NEVER-HERE", out)
		//Assert.assertEquals("ay", file["hello.bin"].readRangeBytes(1L .. 2L).toString(Charsets.UTF_8)) // CompilationException in Kotlin 1.1.1 -> Couldn't transform method node (probably related to long)
		Assert.assertEquals("ay", file["hello.bin"].readRangeBytes(1..2).toString(Charsets.UTF_8))

		Assert.assertEquals("ay!", file["hello.bin"].readRangeBytes(1..200).toString(Charsets.UTF_8))
	}

	@Test
	fun avoidStats() = syncTest {
		val log = LogVfs(MemoryVfsMix("hello.txt" to "yay!"))
		val root = log.root
		root["hello.txt"].readBytes()
		Assert.assertEquals(
			"[readRange(/hello.txt, 0..9223372036854775807)]",
			log.logstr
		)
	}
}