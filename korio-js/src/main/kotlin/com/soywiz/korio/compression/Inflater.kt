package com.soywiz.korio.compression

impl class Inflater impl constructor(val nowrap: Boolean) {
	impl fun needsInput(): Boolean = TODO()
	impl fun setInput(buffer: ByteArray): Unit = TODO()
	impl fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = TODO()
	impl fun end(): Unit = TODO()
}