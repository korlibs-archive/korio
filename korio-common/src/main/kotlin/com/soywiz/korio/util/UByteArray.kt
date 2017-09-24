package com.soywiz.korio.util

class UByteArray(val data: ByteArray) {
	constructor(size: Int) : this(ByteArray(size))

	val size: Int = data.size
	inline operator fun get(n: Int) = this.data[n].toInt() and 0xFF
	inline operator fun set(n: Int, v: Int) = Unit.let { this.data[n] = v.toByte() }
}