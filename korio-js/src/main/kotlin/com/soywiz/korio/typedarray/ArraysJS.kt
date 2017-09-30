package com.soywiz.korio.typedarray

actual object Arrays {
	/*
	actual fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun fill(src: ByteArray, value: Byte, from: Int, to: Int) {
		Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: ShortArray, value: Short, from: Int, to: Int) {
		Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: IntArray, value: Int, from: Int, to: Int) {
		Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: FloatArray, value: Float, from: Int, to: Int) {
		Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: DoubleArray, value: Double, from: Int, to: Int) {
		Arrays.fill(src, from, to, value)
	}
	*/

	actual fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun fill(src: ByteArray, value: Byte, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: ShortArray, value: Short, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: IntArray, value: Int, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: FloatArray, value: Float, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: DoubleArray, value: Double, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}
}