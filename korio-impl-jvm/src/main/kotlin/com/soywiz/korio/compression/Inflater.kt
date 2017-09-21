package com.soywiz.korio.compression

import java.util.zip.Inflater as JInflater

impl class Inflater impl constructor(val nowrap: Boolean) {
	val ji = JInflater(nowrap)

	impl fun needsInput(): Boolean = ji.needsInput()
	impl fun setInput(buffer: ByteArray) = ji.setInput(buffer)
	impl fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = ji.inflate(buffer, offset, len)
	impl fun end() = ji.end()
}