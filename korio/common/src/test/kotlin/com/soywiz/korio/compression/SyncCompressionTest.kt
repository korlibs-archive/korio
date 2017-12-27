package com.soywiz.korio.compression

import com.soywiz.korio.KorioNative
import org.junit.Test
import kotlin.test.assertTrue

class SyncCompressionTest {
	@Test
	fun name() {
		val input = byteArrayOf(1, 1, 1, 1, 1, 1, 1)
		val compressed = KorioNative.SyncCompression.deflate(input, 9)
		val decompressed = KorioNative.SyncCompression.inflate(compressed)
		assertTrue(input.contentEquals(decompressed))
	}
}