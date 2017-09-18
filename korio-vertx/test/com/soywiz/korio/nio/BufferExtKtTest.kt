package com.soywiz.korio.nio

import org.junit.Assert
import org.junit.Test
import java.nio.ByteBuffer

class BufferExtKtTest {
	@Test
	fun name() {
		Assert.assertEquals(
			"Ab",
			ByteBuffer.wrap(byteArrayOf('A'.toByte(), 'b'.toByte())).toString(Charsets.UTF_8)
		)
	}
}