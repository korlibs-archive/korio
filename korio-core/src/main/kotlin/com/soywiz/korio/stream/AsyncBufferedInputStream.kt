package com.soywiz.korio.stream

import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.ds.OptByteBuffer

class AsyncBufferedInputStream(val base: AsyncInputStream, val bufferSize: Int = 0x2000) : AsyncInputStream {
	private val buf = SyncProduceConsumerByteBuffer()

	private val queue = AsyncThread()

	suspend fun require(len: Int = 1) = queue {
		while (buf.available < len) buf.produce(base.readBytes(bufferSize))
	}

	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		require()
		return buf.consume(buffer, offset, len)
	}

	suspend fun readBufferedUntil(end: Byte, including: Boolean = true): ByteArray {
		val out = OptByteBuffer()
		while (true) {
			require()
			val chunk = buf.consumeUntil(end, including)
			out.append(chunk)
			if (chunk.isNotEmpty() && chunk.last() == end) break
		}
		return out.toByteArray()
	}

	suspend override fun close() {
		base.close()
	}
}

fun AsyncInputStream.toBuffered(bufferSize: Int = 0x2000): AsyncBufferedInputStream = AsyncBufferedInputStream(this, bufferSize)