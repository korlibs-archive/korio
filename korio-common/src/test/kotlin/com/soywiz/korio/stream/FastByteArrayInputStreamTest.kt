package com.soywiz.korio.stream

import com.soywiz.korio.lang.format
import org.junit.Test
import kotlin.test.assertEquals

class FastByteArrayInputStreamTest {
	@Test
	fun name() {
		val v = FastByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
		assertEquals(4, v.available)
		assertEquals("01020304", "%08X".format(v.readS32_be()))
		assertEquals(0, v.available)
		assertEquals(4, v.offset)
		assertEquals(4, v.length)
	}
}