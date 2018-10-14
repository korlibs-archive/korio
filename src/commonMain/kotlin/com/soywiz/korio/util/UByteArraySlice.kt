package com.soywiz.korio.util

import com.soywiz.kmem.*

class UByteArraySlice(val data: ByteArray, val position: Int, val length: Int) {
	fun getPointer(): Pointer = Pointer(data, position)
	override fun toString() = "UByteArraySlice(data=$data, position=$position, length=$length)"

	operator fun get(n: Int): Int = data[position + n].toInt() and 0xFF
	operator fun set(n: Int, value: Int): Unit = run { data[position + n] = value.toByte() }

	companion object {
		fun create(start: Pointer, end: Pointer): ByteArraySlice {
			if (start.ba !== end.ba) throw RuntimeException("Pointer must reference the samea array")
			return ByteArraySlice(start.ba, start.offset, end.offset - start.offset)
		}
	}
}

fun ByteArray.toUByteArraySlice() = UByteArraySlice(this, 0, size)

fun ByteArraySlice.toUnsigned() = UByteArraySlice(this.data, position, length)
