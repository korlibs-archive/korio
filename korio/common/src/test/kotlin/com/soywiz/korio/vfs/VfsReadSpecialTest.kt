package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.kds.lmapOf
import com.soywiz.korio.util.expectException
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

	@kotlin.test.Test
	fun testReadSpecial() = syncTest {
		val mem = MemoryVfs(lmapOf())
		assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			mem["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@kotlin.test.Test
	fun testReadSpecial2() = syncTest {
		val mem = MemoryVfs(lmapOf())
		val root = MergedVfs(listOf(mem))
		assertEquals(
			MySpecialClass(mem.vfs, "/test.txt"),
			root["test.txt"].readSpecial<MySpecialClass>()
		)
	}

	@kotlin.test.Test
	fun testReadSpecialNonHandled() = syncTest {
		expectException<Throwable> {
			val mem = MemoryVfs(lmapOf())
			mem["test.txt"].readSpecial<MySpecialClass2>()
		}
	}
}