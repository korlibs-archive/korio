package com.soywiz.korio.compression

actual class Inflater actual constructor(val nowrap: Boolean) {
	actual fun needsInput(): Boolean = TODO()
	actual fun setInput(buffer: ByteArray): Unit = TODO()
	actual fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = TODO()
	actual fun end(): Unit = TODO()
}