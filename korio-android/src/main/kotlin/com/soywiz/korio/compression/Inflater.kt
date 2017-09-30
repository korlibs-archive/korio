package com.soywiz.korio.compression

import java.util.zip.Inflater as JInflater

actual class Inflater actual constructor(val nowrap: Boolean) {
	val ji = JInflater(nowrap)

	actual fun needsInput(): Boolean = ji.needsInput()
	actual fun setInput(buffer: ByteArray) = ji.setInput(buffer)
	actual fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = ji.inflate(buffer, offset, len)
	actual fun end() = ji.end()
}