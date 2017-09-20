package com.soywiz.korio.util

class Pointer(val ba: ByteArray, var offset: Int = 0) : Comparable<Pointer> {
	fun inc() = run { offset++ }
	fun dec() = run { offset-- }

	fun getU8() = ba[offset].toInt() and 0xFF
	fun setU8(v: Int) = run { ba[offset] = v.toByte() }

	fun readU8() = ba[offset++].toInt() and 0xFF
	fun writeU8(v: Int) = run { ba[offset++] = v.toByte() }

	operator fun plus(offset: Int) = Pointer(ba, this.offset + offset)
	operator fun minus(that: Pointer) = this.offset - that.offset
	fun setAdd(that: Pointer, add: Int) {
		this.offset = that.offset + add
	}

	override fun compareTo(other: Pointer): Int = this.offset.compareTo(other.offset)
	fun take(count: Int) = ByteArraySlice(ba, offset, count)

	override fun toString(): String = "Pointer($ba, $offset)"
}