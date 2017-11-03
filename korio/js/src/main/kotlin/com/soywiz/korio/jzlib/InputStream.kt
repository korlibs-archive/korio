package com.soywiz.korio.jzlib

import com.soywiz.korio.IOException

abstract class InputStream {
	open fun available(): Int = 0
	open fun close(): Unit = Unit
	open fun mark(readlimit: Int): Unit = Unit
	open fun markSupported(): Boolean = false
	abstract fun read(): Int
	open fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)
	open fun read(buffer: ByteArray, byteOffset: Int, byteCount: Int): Int {
		var i = 0
		try {
			while (i < byteCount) {
				val c: Int = read()
				if (c == -1) {
					return if (i == 0) -1 else i
				}

				buffer[byteOffset + i] = c.toByte()
				i++
			}
		} catch (e: IOException) {
			if (i != 0) return i
			throw e
		}
		return byteCount
	}

	open fun reset(): Unit = throw IOException("reset")

	open fun skip(n: Long): Long {
		var count = 0L
		for (m in 0 until n) {
			if (read() < 0L) return count
			count++
		}
		return count
	}
}