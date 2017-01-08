package com.soywiz.korio.crypto

import org.junit.Assert
import org.junit.Test

class Base64Test {
	@Test
	fun name() {
		Assert.assertEquals("AQID", Base64.encode(byteArrayOf(1, 2, 3)))
		Assert.assertEquals("aGVsbG8=", Base64.encode("hello".toByteArray()))
		Assert.assertArrayEquals(byteArrayOf(1, 2, 3), Base64.decode("AQID"))
		Assert.assertEquals("hello", Base64.decode("aGVsbG8=").toString(Charsets.UTF_8))
	}
}