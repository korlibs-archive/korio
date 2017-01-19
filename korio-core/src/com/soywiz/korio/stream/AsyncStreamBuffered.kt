package com.soywiz.korio.stream

import java.io.ByteArrayOutputStream

class AsyncStreamBuffered(val s: AsyncStream) : AsyncInputStream {
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = s.read(buffer, offset, len)

	suspend fun readLine(): String {
		val out = ByteArrayOutputStream()
		while (!s.eof()) {
			val c = s.readU8()
			if (c.toChar() == '\n') break
			out.write(c)
		}
		return out.toByteArray().toString(Charsets.UTF_8)
	}
}

fun AsyncStream.toBuffered(): AsyncStreamBuffered = AsyncStreamBuffered(this)