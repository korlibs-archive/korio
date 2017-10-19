package com.soywiz.korio.util

import com.soywiz.korio.lang.format
import com.soywiz.korio.math.reverseBytes
import kotlin.test.assertEquals

class BitsTest {
	@kotlin.test.Test
	fun name() {
		val a = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
		assertEquals("0102030405060708", "%016X".format(a.readS64_be(0)))
		assertEquals("0807060504030201", "%016X".format(a.readS64_le(0)))

		assertEquals("01020304", "%08X".format(a.readS32_be(0)))
		assertEquals("04030201", "%08X".format(a.readS32_le(0)))

		assertEquals("010203", "%06X".format(a.readS24_be(0)))
		assertEquals("030201", "%06X".format(a.readS24_le(0)))

		assertEquals("0102", "%04X".format(a.readS16_be(0)))
		assertEquals("0201", "%04X".format(a.readS16_le(0)))

		assertEquals("01", "%02X".format(a.readS8(0)))

		val data = (0 until 128).map { ((it + 35363) * 104723).toByte() }.toByteArray()
		assertEquals(data.readS64_be(0), data.readS64_le(0).reverseBytes())
		assertEquals(data.readS32_be(0), data.readS32_le(0).reverseBytes())
		assertEquals(data.readU32_be(0).toInt(), data.readU32_le(0).toInt().reverseBytes())
		assertEquals(data.readS16_be(0).toShort(), data.readS16_le(0).toShort().reverseBytes())
		assertEquals(data.readU16_be(0).toShort(), data.readU16_le(0).toShort().reverseBytes())
	}
}