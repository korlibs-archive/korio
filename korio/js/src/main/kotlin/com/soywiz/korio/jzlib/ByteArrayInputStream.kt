package com.soywiz.korio.jzlib

import com.soywiz.korio.lang.System
import com.soywiz.korio.lang.and

open class ByteArrayInputStream : InputStream {
	protected var buf: ByteArray
	protected var pos: Int = 0
	protected var mark: Int = 0
	protected var count: Int = 0

	constructor(buf: ByteArray) {
		this.mark = 0
		this.buf = buf
		this.count = buf.size
	}

	constructor(buf: ByteArray, offset: Int, length: Int) {
		this.buf = buf
		pos = offset
		mark = offset
		count = if (offset + length > buf.size) buf.size else offset + length
	}

	override fun available(): Int = count - pos
	override fun close() = Unit
	override fun mark(readlimit: Int) = run { mark = pos }
	override fun markSupported(): Boolean = true
	override fun read(): Int = if (pos < count) buf[pos++] and 0xFF else -1

	override fun read(buffer: ByteArray, byteOffset: Int, byteCount: Int): Int {
		// Are there any bytes available?
		if (this.pos >= this.count) return -1
		if (byteCount == 0) return 0
		val copylen = if (this.count - pos < byteCount) this.count - pos else byteCount
		System.arraycopy(this.buf, pos, buffer, byteOffset, copylen)
		pos += copylen
		return copylen
	}

	override fun reset() = run { pos = mark }
	override fun skip(byteCount: Long): Long {
		if (byteCount <= 0) return 0
		val temp = pos
		pos = if (this.count - pos < byteCount) this.count else (pos + byteCount).toInt()
		return (pos - temp).toLong()
	}
}
