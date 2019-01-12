package com.soywiz.korio.stream

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlin.test.*

class AsyncBufferedInputStreamTest {
	@Test
	fun test() = suspendTest {
		val buffered = "hello\nworld\ndemo".openAsync().toBuffered()
		assertEquals("hello", buffered.readUntil('\n'.toByte(), including = false).toStringDecimal(UTF8))
		assertEquals("world\n", buffered.readUntil('\n'.toByte(), including = true).toStringDecimal(UTF8))
		assertEquals("demo", buffered.readUntil('\n'.toByte(), including = true).toStringDecimal(UTF8))
	}
}