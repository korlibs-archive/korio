package com.soywiz.korio.steam

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.*
import org.junit.Test
import kotlin.test.assertEquals

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
}