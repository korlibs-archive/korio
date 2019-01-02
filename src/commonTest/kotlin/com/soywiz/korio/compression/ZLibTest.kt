package com.soywiz.korio.compression

import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.compression.deflate.*
import kotlin.test.*

class ZLibTest {
	@Test
	fun name() {
		val data = ByteArray(0x20_000)
		for (n in 0 until data.size) {
			val m = (n * 77) and 0xFF
			val v = when {
				m < 16 -> 0
				m < 32 -> m
				else -> 1
			}
			data[n] = v.toByte()
		}
		val compressedData = data.compress(ZLib)
		val uncompressedData = compressedData.uncompress(ZLib)
		assertEquals(data.size, uncompressedData.size)
		for (n in 0 until data.size) assertEquals(data[n], uncompressedData[n])
	}

	@Test
	fun name2() {
		val data = ByteArray(0x20_000)
		for (n in 0 until data.size) {
			val m = (n * 77) and 0xFF
			val v = when {
				m < 16 -> 0
				m < 32 -> m
				else -> 1
			}
			data[n] = v.toByte()
		}
		val compressedData = data.compress(ZLib)
		val uncompressedData = KorioNative.uncompress(compressedData, data.size, "zlib")
		assertEquals(data.size, uncompressedData.size)
		for (n in 0 until data.size) assertEquals(data[n], uncompressedData[n])
	}
}