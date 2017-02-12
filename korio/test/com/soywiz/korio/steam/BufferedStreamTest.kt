package com.soywiz.korio.steam

import com.soywiz.korio.async.sync
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.*
import org.junit.Assert
import org.junit.Test

class BufferedStreamTest {
	@Test
	fun name() = syncTest {
		val mem = MemorySyncStream().toAsync()
		val write = mem.clone()
		val read = mem.clone().buffered()
		for (n in 0 until 0x10000) write.write8(n)
		for (n in 0 until 0x10000) {
			if (read.readU8() != (n and 0xFF)) Assert.fail()
		}
		Assert.assertEquals(0, read.getAvailable())
		Assert.assertEquals(0, read.readBytes(10).size)
	}
}