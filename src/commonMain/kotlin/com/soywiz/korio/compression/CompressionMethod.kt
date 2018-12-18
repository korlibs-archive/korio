package com.soywiz.korio.compression

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

class CompressionContext(var level: Int = 6) {
	var name: String? = null
	var custom: Any? = null
}

interface CompressionMethod {
	suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream): Unit = unsupported()
	suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext = CompressionContext(level = 6)
	): Unit = unsupported()
}

suspend fun CompressionMethod.compress(
	data: ByteArray,
	context: CompressionContext = CompressionContext(level = 6)
): ByteArray {
	return MemorySyncStreamToByteArraySuspend {
		compress(data.openAsync(), this.toAsync(), context)
	}
}

suspend fun CompressionMethod.uncompress(data: ByteArray): ByteArray {
	val buffer = ByteArrayBuilder(4096)
	val s = MemorySyncStream(buffer)
	uncompress(data.openAsync(), s.toAsync())
	//println("CompressionMethod.uncompress.pre")
	val out = buffer.toByteArray()
	//println("CompressionMethod.uncompress.out:$out")
	return out
	// @TODO: Doesn't work on Kotlin-JS?
	//return MemorySyncStreamToByteArray {
	//	uncompress(data.openAsync(), this.toAsync())
	//}
}

suspend fun CompressionMethod.uncompressTo(data: ByteArray, out: AsyncOutputStream): AsyncOutputStream {
	uncompress(data.openAsync(), out)
	return out
}


object Uncompressed : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream): Unit = run { i.copyTo(o) }
	override suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext
	): Unit = run { i.copyTo(o) }
}

suspend fun ByteArray.uncompress(method: CompressionMethod): ByteArray = method.uncompress(this)
suspend fun ByteArray.compress(
	method: CompressionMethod,
	context: CompressionContext = CompressionContext()
): ByteArray = method.compress(this, context)

fun ByteArray.syncUncompress(method: CompressionMethod): ByteArray = runBlockingNoSuspensions {
	val out = method.uncompress(this)
//	println("ByteArray.syncUncompress: $out")
	out
}

fun ByteArray.syncCompress(method: CompressionMethod, context: CompressionContext = CompressionContext()): ByteArray =
	runBlockingNoSuspensions { method.compress(this, context) }

fun CompressionMethod.syncUncompress(i: SyncInputStream, o: SyncOutputStream) = runBlockingNoSuspensions {
	//println("CompressionMethod.syncUncompress[0]")
	uncompress(i.toAsyncInputStream(), o.toAsyncOutputStream())
	//println("CompressionMethod.syncUncompress[1]")
	//Unit // @TODO: kotlin-js kotlin.js BUG. Passing undefined to the coroutine without this!
}
