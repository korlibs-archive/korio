package com.soywiz.korio.util

import com.soywiz.korio.lang.format
import com.soywiz.korio.math.MathEx
import org.junit.Test
import kotlin.test.assertEquals

class BitsTest {
	@Test
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
		assertEquals(data.readS64_be(0), MathEx.reverseBytes(data.readS64_le(0)))
		assertEquals(data.readS32_be(0), MathEx.reverseBytes(data.readS32_le(0)))
		assertEquals(data.readU32_be(0).toInt(), MathEx.reverseBytes(data.readU32_le(0).toInt()))
		assertEquals(data.readS16_be(0).toShort(), MathEx.reverseBytes(data.readS16_le(0).toShort()))
		assertEquals(data.readU16_be(0).toShort(), MathEx.reverseBytes(data.readU16_le(0).toShort()))
	}
}