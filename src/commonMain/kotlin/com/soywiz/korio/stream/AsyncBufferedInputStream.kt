package com.soywiz.korio.stream

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*

class AsyncBufferedInputStream(val base: AsyncInputStream, val bufferSize: Int = 0x2000) : AsyncInputStream {
	private val buf = SyncProduceConsumerByteBuffer()

	private val queue = AsyncThread()

	suspend fun require(len: Int = 1) = queue {
		while (buf.available < len) {
			buf.produce(base.readBytesUpToFirst(bufferSize))
		}
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		require()
		return buf.consume(buffer, offset, len)
	}

	suspend fun readBufferedUntil(end: Byte, including: Boolean = true, limit: Int = 0x1000): ByteArray {
		val out = ByteArrayBuilder2()
		while (true) {
			require()
			val chunk = buf.consumeUntil(end, including, limit = limit)
			//println("chunk: $chunk, ${chunk.size}")
			out.append(chunk)
			if (chunk.isNotEmpty() && chunk.last() == end) break
		}
		return out.toByteArray()
	}

	suspend fun readUntil(end: Byte, including: Boolean = true, limit: Int = 0x1000): ByteArray =
		readBufferedUntil(end, including, limit)

	override suspend fun close() {
		base.close()
	}
}

suspend fun AsyncBufferedInputStream.readBufferedLine(limit: Int = 0x1000, charset: Charset = UTF8) =
	readBufferedUntil('\n'.toByte(), including = false, limit = limit).toString(charset)

fun AsyncInputStream.toBuffered(bufferSize: Int = 0x2000): AsyncBufferedInputStream =
	AsyncBufferedInputStream(this, bufferSize)