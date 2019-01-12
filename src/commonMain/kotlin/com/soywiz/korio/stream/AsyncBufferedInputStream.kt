package com.soywiz.korio.stream

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*

class AsyncBufferedInputStream(val base: AsyncInputStream, val bufferSize: Int = 0x2000) : AsyncInputStream {
	private val buf = ByteArrayDeque(bufferSize)

	private val queue = AsyncThread()
	private val temp = ByteArray(bufferSize)

	suspend fun require(len: Int = 1) = queue {
		while (buf.availableRead < len) {
			val read = base.read(temp, 0, temp.size)
			if (read <= 0) break
			buf.write(temp, 0, read)
		}
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		require()
		return buf.read(buffer, offset, len)
	}

	override suspend fun read(): Int {
		require()
		return buf.readByte()
	}

	suspend fun readUntil(end: Byte, including: Boolean = true, limit: Int = 0x1000): ByteArray {
		val out = ByteArrayBuilder()
		while (true) {
			require()
			val byteInt = buf.readByte()
			if (byteInt < 0) break
			val byte = byteInt.toByte()
			//println("chunk: $chunk, ${chunk.size}")
			if (including || byte != end) {
				out.append(byte)
			}
			if (byte == end || out.size >= limit) break
		}
		return out.toByteArray()
	}

	override suspend fun close() {
		base.close()
	}
}

suspend fun AsyncBufferedInputStream.readBufferedLine(limit: Int = 0x1000, charset: Charset = UTF8) =
	readUntil('\n'.toByte(), including = false, limit = limit).toStringDecimal(charset)

fun AsyncInputStream.toBuffered(bufferSize: Int = 0x2000): AsyncBufferedInputStream =
	AsyncBufferedInputStream(this, bufferSize)

