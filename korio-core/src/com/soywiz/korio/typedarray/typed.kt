package com.soywiz.korio.typedarray

//header object ByteArrayBliting {
//	fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int)
//}

// @TODO: Use header to optimize per platform!
object ByteArrayBliting {
	fun fill(bytes: ByteArray, value: Byte, from: Int = 0, to: Int = bytes.size) {
		for (n in from until to) bytes[n] = value
	}
}

fun ByteArray.copyRangeTo(srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
	for (n in 0 until count) dst[dstPos + n] = this[srcPos + n]
}

fun ByteArray.fill(value: Byte, from: Int = 0, to: Int = this.size) {
	ByteArrayBliting.fill(this, value, from, to)
}

/*
// @TODO: Implement JS Typed Buffers
header class ArrayBuffer {
	companion object {
		//fun
	}
}

interface TypedArray {
}
*/