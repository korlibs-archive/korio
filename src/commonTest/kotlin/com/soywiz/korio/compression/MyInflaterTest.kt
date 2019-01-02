package com.soywiz.korio.compression

import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.test.*

class MyInflaterTest {
	@Test
	fun test() {
		val result = "CB 48 CD C9 C9 57 28 CF 2F CA 49 01 00".unhexIgnoreSpaces.uncompress(MyDeflate)
		assertEquals("hello world", result.toString(UTF8))
	}

	@Test
	fun test2() {
		val result = "CB 48 CD C9 C9 57 28 CF 2F CA 49 D1 51 C8 20 86 03 00".unhexIgnoreSpaces.uncompress(MyDeflate)
		assertEquals("hello world, hello world, hello world, hello world", result.toString(UTF8))
	}

	@Test
	fun test3() {
		val compressed = "ED C2 31 0D 00 00 00 02 A0 4A 06 B2 7F 0E 53 F8 31 68 0A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 9C 0D".unhexIgnoreSpaces
		val result = compressed.uncompress(MyDeflate)
		val decompressor = MyDeflate.createDecompresor()
		for (n in 0 until 1000) {
			compressed.process(decompressor)
		}
		assertEquals(0x5000, result.size)
		assertEquals("\\0".repeat(0x2800), result.toString(UTF8))
	}
}


