package com.soywiz.korio.compression

import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.compression.lzma.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.test.*

class ZLibTest {
	val data = ByteArray(0x20_000).apply {
		for (n in 0 until this.size) {
			val m = (n * 77) and 0xFF
			val v = when {
				m < 16 -> 0
				m < 32 -> m
				else -> 1
			}
			this[n] = v.toByte()
		}
	}

	@Test
	fun test() {
		assertEquals("hello", "78 DA CB 48 CD C9 C9 07 00 06 2C 02 15".unhexIgnoreSpaces.uncompress(ZLib).toString(UTF8))
		assertEquals("hello", "1F 8B 08 00 00 00 00 00 02 13 CB 48 CD C9 C9 07 00 86 A6 10 36 05 00 00 00".unhexIgnoreSpaces.uncompress(GZIP).toString(UTF8))
	}

	@Test
	fun zlibCompressDecompress() {
		val compressedData = data.compress(ZLib)
		val uncompressedData = compressedData.uncompress(ZLib)
		assertEquals(data.size, uncompressedData.size)
		for (n in 0 until data.size) assertEquals(data[n], uncompressedData[n])
	}

	@Test
	fun gzipCompressDecompress() {
		val compressedData = data.compress(GZIP)
		val uncompressedData = compressedData.uncompress(GZIP)
		assertEquals(data.size, uncompressedData.size)
		for (n in 0 until data.size) assertEquals(data[n], uncompressedData[n])
	}

	@Test
	fun deflateCompressDecompress() {
		val compressedData = data.compress(Deflate)
		val uncompressedData = compressedData.uncompress(Deflate)
		assertEquals(data.size, uncompressedData.size)
		for (n in 0 until data.size) assertEquals(data[n], uncompressedData[n])
	}

	@Test
	fun deflatePortableCompressDecompress() {
		val compressedData = data.compress(DeflatePortable)
		val uncompressedData = compressedData.uncompress(DeflatePortable)
		assertEquals(data.size, uncompressedData.size)
		for (n in 0 until data.size) assertEquals(data[n], uncompressedData[n])
	}

	@Test
	fun lzmaCompressDecompress() {
		val compressedData = data.compress(Lzma)
		val uncompressedData = compressedData.uncompress(Lzma)
		assertEquals(data.size, uncompressedData.size)
		for (n in 0 until data.size) assertEquals(data[n], uncompressedData[n])
	}
}