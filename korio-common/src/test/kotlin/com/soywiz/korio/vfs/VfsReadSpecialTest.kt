package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.util.expectException
import org.junit.Test
import kotlin.test.assertEquals

data class MySpecialClass(val vfs: Vfs, val path: String)
data class MySpecialClass2(val vfs: Vfs, val path: String)

class MySpecialClass_VfsSpecialReader : VfsSpecialReader<MySpecialClass>(MySpecialClass::class) {
	override suspend fun readSpecial(vfs: Vfs, path: String): MySpecialClass = MySpecialClass(vfs, path)
}

class VfsReadSpecialTest {
	init {
		registerVfsSpecialReader(MySpecialClass_VfsSpecialReader())
	}

	@Test
	fun testReadSpecial() = syncTest {
		val mem = MemoryVfs(mapOf())
		assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			mem["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@Test
	fun testReadSpecial2() = syncTest {
		val mem = MemoryVfs(mapOf())
		val root = MergedVfs(listOf(mem))
		assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			root["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@Test
	fun testReadSpecialNonHandled() = syncTest {
		expectException<Throwable> {
			val mem = MemoryVfs(mapOf())
			mem["test.txt"].readSpecial<MySpecialClass2>()
		}
	}
}