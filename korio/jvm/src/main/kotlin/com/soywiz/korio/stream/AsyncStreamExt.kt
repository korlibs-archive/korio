package com.soywiz.korio.stream

import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.error.unsupported
import java.io.InputStream

fun InputStream.toAsync(length: Long? = null): AsyncInputStream {
	val syncIS = this
	if (length != null) {
		return object : AsyncInputStream, AsyncLengthStream {
			suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker {
				syncIS.read(buffer, offset, len)
			}
			override suspend fun close() = syncIS.close()
			suspend override fun setLength(value: Long) {
				unsupported("Can't set length")
			}
			suspend override fun getLength(): Long = length
		}
	} else {
		return object : AsyncInputStream {
			suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker {
				syncIS.read(buffer, offset, len)
			}

			override suspend fun close() {
				syncIS.close()
			}
		}
	}
}
