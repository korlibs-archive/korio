package com.soywiz.korio.ds

import com.soywiz.korio.util.toHexStringLower
import org.junit.Assert
import org.junit.Test

class OptByteBufferTest {
	@Test
	fun name() {
		val bb = OptByteBuffer()
		bb.append(byteArrayOf(1))
		bb.append(byteArrayOf(2, 3))
		bb.append(byteArrayOf(4))
		Assert.assertEquals("01020304", bb.toByteArray().toHexStringLower())
	}
}