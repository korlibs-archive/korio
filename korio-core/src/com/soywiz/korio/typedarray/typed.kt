package com.soywiz.korio.typedarray

//header object ByteArrayBliting {
//	fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int)
//}
object ByteArrayBliting {
	fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}
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