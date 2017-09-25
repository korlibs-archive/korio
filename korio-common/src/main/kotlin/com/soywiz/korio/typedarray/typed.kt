package com.soywiz.korio.typedarray

header fun ByteArray.copyRangeTo(srcPos: Int, dst: ByteArray, dstPos: Int, count: Int): Unit
header fun ByteArray.fill(value: Byte, from: Int, to: Int): Unit

fun ByteArray.fill(value: Byte): Unit = fill(value, 0, this.size)

// @TODO: DEFAULT IMPLEMENTATION (not working)
//impl fun ByteArray.copyRangeTo(srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
//	for (n in 0 until count) dst[dstPos + n] = this[srcPos + n]
//}
//
//impl fun ByteArray.fill(value: Byte, from: Int, to: Int) {
//	for (n in from until to) this[n] = value
//}

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