package com.soywiz.korio.steam

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class BufferedStreamTest {
	@Test
	fun name() = syncTest {
		val mem = MemorySyncStream().toAsync()
		val write = mem.duplicate()
		val read = mem.duplicate().buffered()
		for (n in 0 until 0x10000) write.write8(n)
		for (n in 0 until 0x10000) {
			if (read.readU8() != (n and 0xFF)) fail()
		}
		assertEquals(0, read.getAvailable())
		assertEquals(0, read.readBytes(10).size)
	}
}