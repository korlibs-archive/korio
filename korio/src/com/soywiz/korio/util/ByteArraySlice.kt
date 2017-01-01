package com.soywiz.korio.util

import java.util.*

class ByteArraySlice(val data: ByteArray, val position: Int, val length: Int) {
	fun getPointer(): Pointer = Pointer(data, position)
	override fun toString() = "ByteArraySlice(data=$data, position=$position, length=$length)"

	operator fun get(n: Int): Byte = data[position + n]
	operator fun set(n: Int, value: Byte): Unit = run { data[position + n] = value }

	companion object {
		fun create(start: Pointer, end: Pointer): ByteArraySlice {
			if (start.ba != end.ba) throw RuntimeException("Pointer must reference the samea array")
			return ByteArraySlice(start.ba, start.offset, end.offset - start.offset)
		}
	}
}

fun ByteArray.toByteArraySlice() = ByteArraySlice(this, 0, size)

fun ByteArray.copyOf(newLength: Int) = Arrays.copyOf(this, newLength)
fun ByteArray.copyOfRange(from: Int, to: Int) = Arrays.copyOfRange(this, from, to)