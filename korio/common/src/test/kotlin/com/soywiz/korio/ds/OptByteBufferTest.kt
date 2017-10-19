package com.soywiz.korio.ds

import com.soywiz.korio.util.toHexStringLower
import kotlin.test.assertEquals

class OptByteBufferTest {
	@kotlin.test.Test
	fun name() {
		val bb = ByteArrayBuilder()
		bb.append(byteArrayOf(1))
		bb.append(byteArrayOf(2, 3))
		bb.append(byteArrayOf(4))
		assertEquals("01020304", bb.toByteArray().toHexStringLower())
	}
}