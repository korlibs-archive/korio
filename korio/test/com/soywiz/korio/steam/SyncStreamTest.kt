package com.soywiz.korio.steam

import com.soywiz.korio.async.sync
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.ByteArrayBuffer
import org.junit.Assert
import org.junit.Test

class SyncStreamTest {
	@Test
	fun name() {
		val buffer = ByteArrayBuffer()
		val out = MemorySyncStream(buffer)
		Assert.assertEquals(0L, out.position)
		Assert.assertEquals(0L, out.length)
		out.write8(0x01)
		out.write8(0x02)
		out.write16_le(0x0304)
		Assert.assertEquals(4L, out.position)
		Assert.assertEquals(4L, out.length)
		out.position = 0L
		Assert.assertEquals(0L, out.position)
		Assert.assertEquals(4L, out.length)
		Assert.assertEquals(0x0102, out.readU16_be())
		Assert.assertEquals(0x0304, out.readU16_le())
		Assert.assertEquals(14, buffer.data.size)
		Assert.assertEquals(4, buffer.toByteArray().size)
		Assert.assertEquals(4, buffer.toByteArraySlice().length)
	}

	@Test
	fun testArrays() = syncTest {
		val out = MemorySyncStream()
		for (n in 0 until 6) out.write32_le(n * n)
		out.position = 0L
		Assert.assertEquals(
			"[0, 1, 4, 9, 16, 25]", out.readIntArray_le(6).toList().toString()
		)
		out.position = 0L
		out.writeIntArray_le(intArrayOf(-1, -2, -3, -4, -5, -6, -7, -8))
		out.position = 0L
		Assert.assertEquals(
			"[-1, -2, -3, -4, -5, -6, -7, -8]",
			out.readIntArray_le(8).toList().toString()
		)
	}

	@Test
	fun test2() = syncTest {
		val out = MemorySyncStream()
		out.write16_be(0x1234)
		val bb = out.toByteArray()
		Assert.assertArrayEquals(byteArrayOf(0x12, 0x34), bb)
	}
}