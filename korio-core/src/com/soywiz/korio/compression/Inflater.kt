package com.soywiz.korio.compression

class Inflater(val b: Boolean) {
	fun needsInput(): Boolean = TODO()
	fun setInput(buffer: ByteArray): Unit = TODO()
	fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = TODO()
	fun end(): Unit = TODO()
}