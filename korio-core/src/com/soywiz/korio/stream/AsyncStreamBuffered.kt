package com.soywiz.korio.stream

import com.soywiz.korio.async.asyncFun
import java.io.ByteArrayOutputStream

class AsyncStreamBuffered(val s: AsyncStream) : AsyncInputStream {
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
		s.read(buffer, offset, len)
	}

	suspend fun readLine(): String = asyncFun {
		val out = ByteArrayOutputStream()
		while (!s.eof()) {
			val c = s.readU8()
			if (c.toChar() == '\n') break
			out.write(c)
		}
		out.toByteArray().toString(Charsets.UTF_8)
	}
}

fun AsyncStream.toBuffered(): AsyncStreamBuffered = AsyncStreamBuffered(this)