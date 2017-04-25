package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

data class MySpecialClass(val vfs: Vfs, val path: String)
data class MySpecialClass2(val vfs: Vfs, val path: String)

class MySpecialClass_VfsSpecialReader : VfsSpecialReader<MySpecialClass>(MySpecialClass::class.java) {
	override fun readSpecial(vfs: Vfs, path: String): MySpecialClass = MySpecialClass(vfs, path)
}

class VfsReadSpecialTest {
	@Test
	fun testReadSpecial() = syncTest {
		val mem = MemoryVfs(mapOf())
		Assert.assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			mem["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@Test
	fun testReadSpecial2() = syncTest {
		val mem = MemoryVfs(mapOf())
		val root = MergedVfs(listOf(mem))
		Assert.assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			root["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@Test(expected = Throwable::class)
	fun testReadSpecialNonHandled() = syncTest {
		val mem = MemoryVfs(mapOf())
		mem["test.txt"].readSpecial<MySpecialClass2>()
	}
}