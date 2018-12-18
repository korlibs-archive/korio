package com.soywiz.korio.compression.lzma

import com.soywiz.korio.compression.*
import com.soywiz.korio.stream.*

/**
 * @TODO: Streaming! (right now loads the whole stream in-memory)
 */
object Lzma : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		val input = i.readAll().openSync()
		val properties = input.readBytesExact(5)
		val decoder = SevenZip.LzmaDecoder()
		if (!decoder.SetDecoderProperties(properties)) throw Exception("Incorrect stream properties")
		val outSize = input.readS64LE()
		val out = MemorySyncStreamToByteArray {
			if (!decoder.Code(input, this, outSize)) throw Exception("Error in data stream")
		}
		o.writeBytes(out)
	}

	override suspend fun compress(i: AsyncInputWithLengthStream, o: AsyncOutputStream, context: CompressionContext) {
		val algorithm = 2
		val matchFinder = 1
		val dictionarySize = 1 shl 23
		val lc = 3
		val lp = 0
		val pb = 2
		val fb = 128
		val eos = false

		val input = i.readAll()

		val out = MemorySyncStreamToByteArray {
			val encoder = SevenZip.LzmaEncoder()
			if (!encoder.SetAlgorithm(algorithm)) throw Exception("Incorrect compression mode")
			if (!encoder.SetDictionarySize(dictionarySize))
				throw Exception("Incorrect dictionary size")
			if (!encoder.SetNumFastBytes(fb)) throw Exception("Incorrect -fb value")
			if (!encoder.SetMatchFinder(matchFinder)) throw Exception("Incorrect -mf value")
			if (!encoder.SetLcLpPb(lc, lp, pb)) throw Exception("Incorrect -lc or -lp or -pb value")
			encoder.SetEndMarkerMode(eos)
			encoder.WriteCoderProperties(this)
			val fileSize: Long = if (eos) -1 else input.size.toLong()
			this.write64LE(fileSize)
			encoder.Code(input.openSync(), this, -1, -1, null)
		}

		o.writeBytes(out)
	}
}