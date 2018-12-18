package com.soywiz.korio.compression.util

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

class CompressionAlgoAsyncStream internal constructor(
	val i: AsyncInputWithLengthStream,
	val method: CompressionMethod,
	val uncompressedSize: Long? = null,
	val compressing: Boolean = false
) :
	AsyncInputWithLengthStream {

	val los = LimitedOutputStream()
	private var pos = 0L

	internal suspend fun init() {
		launchImmediately(coroutineContext) {
			if (compressing) {
				method.compress(i, los)
			} else {
				method.uncompress(i, los)
			}
		}
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		val ret = los.provideOutput(buffer, offset, len)
		if (ret > 0) pos += ret
		return ret
	}

	override suspend fun getPosition(): Long = pos
	override suspend fun getLength(): Long = uncompressedSize ?: throw UnsupportedOperationException()
	override suspend fun close() {
		//inflater.end()
	}
}

suspend fun UncompressAsyncStream(
	mode: CompressionMethod,
	i: AsyncInputWithLengthStream,
	uncompressedSize: Long? = null
): AsyncInputWithLengthStream {
	return CompressionAlgoAsyncStream(i, mode, uncompressedSize, compressing = false).apply {
		init()
	}
}

suspend fun CompressAsyncStream(
	mode: CompressionMethod,
	i: AsyncInputWithLengthStream
): AsyncInputWithLengthStream {
	return CompressionAlgoAsyncStream(i, mode, compressing = true).apply {
		init()
	}
}

class LimitedOutputStream : AsyncOutputStream {
	private data class ByteArraySlice(val data: ByteArray, val position: Int, val length: Int)

	private class Task(val slice: ByteArraySlice) {
		val count = CompletableDeferred<Int>(Job())
	}

	private val queue = ProduceConsumer<Task>()

	override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
		//println("write: $len")
		if (len == 0) return // Ignore empty chunks
		var o = offset
		var pending = len
		while (pending > 0) {
			val task = queue.consume() ?: invalidOp("Couldn't read from stream")
			val toRead = min(pending, task.slice.length)
			arraycopy(buffer, o, task.slice.data, task.slice.position, toRead)
			//println("write...: $pending, ${task.slice.length}, $toRead")
			task.count.complete(toRead)
			pending -= toRead
			o += toRead
		}
	}

	override suspend fun close() {
		queue.close()
	}

	suspend fun provideOutput(buffer: ByteArray, offset: Int, len: Int): Int {
		val task = Task(ByteArraySlice(buffer, offset, len))
		queue.produce(task)
		//println("provideOutput: $len")
		val result = task.count.await()
		//println("provideOutput: $len --> $result")
		return result
	}
}
