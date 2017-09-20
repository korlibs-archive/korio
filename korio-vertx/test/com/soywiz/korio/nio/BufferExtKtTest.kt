package com.soywiz.korio.nio

import org.junit.Test
import kotlin.test.assertEquals

class BufferExtKtTest {
	@Test
	fun name() {
		assertEquals(
			"Ab",
			ByteBuffer.wrap(byteArrayOf('A'.toByte(), 'b'.toByte())).toString(Charsets.UTF_8)
		)
	}
}