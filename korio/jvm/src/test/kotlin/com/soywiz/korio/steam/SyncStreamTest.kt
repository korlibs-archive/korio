package com.soywiz.korio.steam

import com.soywiz.kmem.ByteArrayBuffer
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.*
import org.junit.Test
import kotlin.test.assertEquals

class SyncStreamTest {
	@Test
	fun name() {
		val buffer = ByteArrayBuffer()
		val out = MemorySyncStream(buffer)
		assertEquals(0L, out.position)
		assertEquals(0L, out.length)
		out.write8(0x01)
		out.write8(0x02)
		out.write16_le(0x0304)
		assertEquals(4L, out.position)
		assertEquals(4L, out.length)
		out.position = 0L
		assertEquals(0L, out.position)
		assertEquals(4L, out.length)
		assertEquals(0x0102, out.readU16_be())
		assertEquals(0x0304, out.readU16_le())
		assertEquals(4096, buffer.data.size)
		assertEquals(4, buffer.toByteArray().size)
		assertEquals(4, buffer.toByteArraySlice().length)
	}

	@Test
	fun testArrays() = syncTest {
		val out = MemorySyncStream()
		for (n in 0 until 6) out.write32_le(n * n)
		out.position = 0L
		assertEquals(
			"[0, 1, 4, 9, 16, 25]", out.readIntArray_le(6).toList().toString()
		)
		out.position = 0L
		out.writeIntArray_le(intArrayOf(-1, -2, -3, -4, -5, -6, -7, -8))
		out.position = 0L
		assertEquals(
			"[-1, -2, -3, -4, -5, -6, -7, -8]",
			out.readIntArray_le(8).toList().toString()
		)
	}

	@Test
	fun test2() = syncTest {
		val out = MemorySyncStream()
		out.write16_be(0x1234)
		val bb = out.toByteArray()
		assertEquals(byteArrayOf(0x12, 0x34).toList(), bb.toList())
	}

	@Test
	fun testUVL() {
		val values = listOf(0, 1, 33, 127, 128, 255, 256, 1985, 91234, 2131231, Int.MAX_VALUE)
		val out = MemorySyncStream()
		for (v in values) out.writeU_VL(v)
		out.position = 0
		val readValues = (0 until values.size).map { out.readU_VL() }
		assertEquals(values, readValues)
	}

	@Test
	fun testSVL() {
		val values = listOf(Int.MIN_VALUE, -2131231, -91234, -1985, -256, -255, -128, -127, -33, -1, 0, 1, 33, 127, 128, 255, 256, 1985, 91234, 2131231, Int.MAX_VALUE)
		val out = MemorySyncStream()
		for (v in values) out.writeS_VL(v)
		out.position = 0
		val readValues = (0 until values.size).map { out.readS_VL() }
		assertEquals(values, readValues)
	}
}