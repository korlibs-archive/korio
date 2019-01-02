package com.soywiz.korio.compression

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*
import kotlin.math.*

open class CompressionContext(var level: Int = 6) {
	var name: String? = null
	var custom: Any? = null
}

interface CompressionMethod {
	suspend fun uncompress(i: AsyncInputStreamWithLength, o: AsyncOutputStream): Unit = unsupported()
	suspend fun compress(
		i: AsyncInputStreamWithLength,
		o: AsyncOutputStream,
		context: CompressionContext = CompressionContext()
	): Unit = unsupported()

	object Uncompressed : CompressionMethod {
		override suspend fun uncompress(i: AsyncInputStreamWithLength, o: AsyncOutputStream): Unit = run { i.copyTo(o) }
		override suspend fun compress(i: AsyncInputStreamWithLength, o: AsyncOutputStream, context: CompressionContext): Unit = run { i.copyTo(o) }
	}
}

suspend fun CompressionMethod.uncompressStream(input: AsyncInputStreamWithLength): AsyncInputStream = asyncStreamWriter { output -> uncompress(input, output) }
suspend fun CompressionMethod.compressStream(input: AsyncInputStreamWithLength): AsyncInputStream = asyncStreamWriter { output -> compress(input, output) }

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

