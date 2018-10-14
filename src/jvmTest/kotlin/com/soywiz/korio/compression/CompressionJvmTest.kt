package com.soywiz.korio.compression

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.lang.*
import kotlin.test.*

class CompressionJvmTest {
	val compressedData =
		Base64.decode("H4sIAAAAAAAAA+3SsREAEBSD4WcFm2ACTID9dxGFxgDcub/4mjQpEmdmDuYPKwsSJT3qz1KkXu7fWZMu4/IGr78AAAAAAD+a6ywcnAAQAAA=")
	val expectedData = "" +
			"1111111111111111111111111111111111111111111111111111111111111111111818181814950511111111111111111111111111818181816566671111111" +
			"1111111111111111118181811818283111111111111111111111111118181111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
			"111111111111111111111111111111"

	@Test
	fun gzipAsync() = suspendTest {
		val data = compressedData
		val res = data.uncompress(GZIPNoCrc)
		val res2 = res.readIntArray_le(0, 4096 / 4)
		val actualData = res2.toList().joinToString("")
		if (expectedData != actualData) {
			println("EX: $expectedData")
			println("AC: $actualData")
		}
		assertEquals(expectedData, actualData)
	}

	@Test
	fun gzipSync() {
		val data = compressedData
		val res = data.syncUncompress(GZIPNoCrc)
		val res2 = res.readIntArray_le(0, 4096 / 4)
		assertEquals(expectedData, res2.toList().joinToString(""))
	}

	@Test
	fun compressGzipSync() = compressSync(GZIP)

	@Test
	fun compressZlibSync() = compressSync(ZLib)

	@Test
	fun compressDeflateSync() = compressSync(Deflate)

	fun compressSync(method: CompressionMethod) {
		val str = "HELLO HELLO HELLO!"
		val uncompressed = str.toByteArray(UTF8)
		val compressed = uncompressed.syncCompress(method)
		val decompressed = compressed.syncUncompress(method)
		assertEquals(decompressed.toString(UTF8), str, "With $method")
	}
}
