package com.soywiz.korio.util

class ByteArraySlice(val data: ByteArray, val position: Int, val length: Int) {
	fun getPointer(): Pointer = Pointer(data, position)
	override fun toString() = "ByteArraySlice(data=$data, position=$position, length=$length)"

	companion object {
		fun create(start: Pointer, end: Pointer): ByteArraySlice {
			if (start.ba != end.ba) throw RuntimeException("Pointer must reference the samea array")
			return ByteArraySlice(start.ba, start.offset, end.offset - start.offset)
		}
	}
}