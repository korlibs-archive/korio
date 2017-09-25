package com.soywiz.korio.typedarray

// @TODO: Use Typed arrays .set for faster performance
// @TODO: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/TypedArray/set
impl fun ByteArray.copyRangeTo(srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
	val src = this
	for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	//this.asDynamic().set()
}

impl fun ByteArray.fill(value: Byte, from: Int, to: Int) {
	for (n in from until to) this[n] = value
	//this.asDynamic().fill(value, from, to)
}
