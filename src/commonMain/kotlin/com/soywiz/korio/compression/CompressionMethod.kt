package com.soywiz.korio.compression

import com.soywiz.korio.async.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

open class CompressionContext(var level: Int = 6) {
	var name: String? = null
	var custom: Any? = null
}

interface CompressionMethod {
	suspend fun uncompress(reader: BitReader, out: AsyncOutputStream): Unit = unsupported()
	suspend fun compress(
		i: BitReader,
		o: AsyncOutputStream,
		context: CompressionContext = CompressionContext()
	): Unit = unsupported()

	object Uncompressed : CompressionMethod {
		override suspend fun uncompress(reader: BitReader, out: AsyncOutputStream): Unit = run { reader.copyTo(out) }
		override suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext): Unit = run { i.copyTo(o) }
	}
}

suspend fun CompressionMethod.uncompress(i: AsyncInputStreamWithLength, o: AsyncOutputStream): Unit = uncompress(BitReader(i), o)
suspend fun CompressionMethod.compress(i: AsyncInputStreamWithLength, o: AsyncOutputStream, context: CompressionContext = CompressionContext()): Unit = compress(BitReader(i), o, context)

suspend fun CompressionMethod.uncompressStream(input: AsyncInputStreamWithLength): AsyncInputStream = asyncStreamWriter { output -> uncompress(input, output) }
suspend fun CompressionMethod.compressStream(input: AsyncInputStreamWithLength, context: CompressionContext = CompressionContext()): AsyncInputStream = asyncStreamWriter { output -> compress(input, output, context) }

fun CompressionMethod.uncompress(i: SyncInputStream, o: SyncOutputStream) {
	runBlockingNoSuspensions {
		this@uncompress.uncompress(i.toAsyncInputStream(), o.toAsyncOutputStream())
	}
}

fun CompressionMethod.compress(i: SyncInputStream, o: SyncOutputStream, context: CompressionContext = CompressionContext()) {
	runBlockingNoSuspensions {
		this@compress.compress(i.toAsyncInputStream(), o.toAsyncOutputStream(), context)
	}
}

fun ByteArray.uncompress(method: CompressionMethod): ByteArray = MemorySyncStreamToByteArray { method.uncompress(this@uncompress.openSync(), this) }
fun ByteArray.compress(method: CompressionMethod, context: CompressionContext = CompressionContext()): ByteArray =
	MemorySyncStreamToByteArray { method.compress(this@compress.openSync(), this, context) }

suspend fun AsyncInputStreamWithLength.uncompressed(method: CompressionMethod): AsyncInputStream = method.uncompressStream(this)
suspend fun AsyncInputStreamWithLength.compressed(method: CompressionMethod, context: CompressionContext = CompressionContext()): AsyncInputStream = method.compressStream(this, context)
