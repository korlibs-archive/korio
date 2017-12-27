package com.soywiz.korio.steam

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsyncStreamTest {
	@Test
	fun name() = syncTest {
		val mem = FillSyncStream(0).toAsync()
		println(mem.readU8())
	}

	@Test
	fun name2() = syncTest {
		val data = "HELLO WORLD\u0000TEST".toByteArray()
		assertEquals("HELLO WORLD", data.openAsync().readStringz())
	}

	@Test
	fun name3() = syncTest {
		val bytes = "HELLO WORLD\u0000TEST".toByteArray()
		val data = bytes.openAsync()
		data.position = 1000
		assertEquals(listOf(), data.readBytesUpTo(20).toList())
		data.position = bytes.size.toLong()
		assertEquals(listOf(), data.readBytesUpTo(20).toList())
		data.position = bytes.size.toLong() - 1
		assertEquals(listOf('T'.toByte()), data.readBytesUpTo(20).toList())
	}

	@Test
	fun name4() = syncTest {
		assertTrue(byteArrayOf(1, 2, 3).openSync().toAsync().base is MemoryAsyncStreamBase)
		assertTrue(byteArrayOf(1, 2, 3).openAsync().base is MemoryAsyncStreamBase)
	}
}