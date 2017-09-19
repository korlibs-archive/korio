package com.soywiz.korio.steam

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.*
import org.junit.Assert
import org.junit.Test

class AsyncStreamTest {
	@Test
	fun name() = syncTest {
		val mem = FillSyncStream(0).toAsync()
		println(mem.readU8())
	}

	@Test
	fun name2() = syncTest {
		val data = "HELLO WORLD\u0000TEST".toByteArray()
		Assert.assertEquals("HELLO WORLD", data.openAsync().readStringz())
	}
}