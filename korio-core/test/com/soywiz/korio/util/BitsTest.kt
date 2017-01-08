package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Test

class BitsTest {
	@Test
	fun name() {
		val data = (0 until 128).map { ((it + 35363) * 104723).toByte() }.toByteArray()
		Assert.assertEquals(data.readS64_be(0), java.lang.Long.reverseBytes(data.readS64_le(0)))
		Assert.assertEquals(data.readS32_be(0), java.lang.Integer.reverseBytes(data.readS32_le(0)))
		Assert.assertEquals(data.readU32_be(0).toInt(), java.lang.Integer.reverseBytes(data.readU32_le(0).toInt()))
		Assert.assertEquals(data.readS16_be(0).toShort(), java.lang.Short.reverseBytes(data.readS16_le(0).toShort()))
		Assert.assertEquals(data.readU16_be(0).toShort(), java.lang.Short.reverseBytes(data.readU16_le(0).toShort()))
	}
}