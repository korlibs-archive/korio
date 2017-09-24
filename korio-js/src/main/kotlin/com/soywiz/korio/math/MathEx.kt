package com.soywiz.korio.math

import org.khronos.webgl.*

impl object MathEx {
	val arrayBuffer = ArrayBuffer(16)
	val f32 = Float32Array(arrayBuffer)
	val i32 = Int32Array(arrayBuffer)
	val f64 = Float64Array(arrayBuffer)

	// @TODO: Untested
	fun makeInt64(low: Int, high: Int): Long = (high.toLong() shl 32) or (low.toLong() and 0xFFFFFFFF)

	impl fun floatToIntBits(v: Float): Int {
		f32[0] = v
		return i32[0]
	}
	impl fun doubleToLongBits(v: Double): Long {
		f64[0] = v
		// @TODO: Untested
		return makeInt64(i32[0], i32[1])
	}
	impl fun intBitsToFloat(v: Int): Float {
		i32[0] = v
		return f32[0]
	}
	impl fun longBitsToDouble(v: Long): Double {
		i32[0] = ((v ushr 0) and 0xFFFFFFFF).toInt()
		i32[1] = ((v ushr 32) and 0xFFFFFFFF).toInt()
		return f64[0]
	}

	impl fun reverseBytes(v: Long): Long {
		val low = (v ushr 0) and 0xFFFFFFFF
		val high = (v ushr 32) and 0xFFFFFFFF
		return ((high shl 32) or low)
	}
	impl fun reverseBytes(v: Int): Int {
		val v0 = (v ushr 0) and 0xFF
		val v1 = (v ushr 8) and 0xFF
		val v2 = (v ushr 16) and 0xFF
		val v3 = (v ushr 24) and 0xFF
		return ((v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0))
	}
	impl fun reverseBytes(v: Short): Short {
		val v0 = (v.toInt() ushr 0) and 0xFF
		val v1 = (v.toInt() ushr 8) and 0xFF
		return ((v0 shl 8) or (v1 shl 0)).toShort()
	}
}