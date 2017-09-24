package com.soywiz.korio.compression

header class Inflater(nowrap: Boolean) {
	fun needsInput(): Boolean
	fun setInput(buffer: ByteArray): Unit
	fun inflate(buffer: ByteArray, offset: Int, len: Int): Int
	fun end(): Unit
}