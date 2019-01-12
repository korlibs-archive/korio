package com.soywiz.korio.crypto

import com.soywiz.korio.lang.*
import com.soywiz.korio.util.encoding.*
import kotlin.test.*

class Base64Test {
	@Test
	fun name() {
		assertEquals("AQID", Base64.encode(byteArrayOf(1, 2, 3)))
		assertEquals("aGVsbG8=", Base64.encode("hello".toByteArray()))
		assertEquals("aGVsbG8=", Base64.encode("hello", UTF8))
		assertEquals(byteArrayOf(1, 2, 3).toList(), Base64.decode("AQID").toList())
		assertEquals("hello", Base64.decode("aGVsbG8=").toStringDecimal(UTF8))
	}
}