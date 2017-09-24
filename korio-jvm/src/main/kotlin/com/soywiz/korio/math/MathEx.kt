package com.soywiz.korio.math

impl object MathEx {
	impl fun floatToIntBits(v: Float): Int = java.lang.Float.floatToIntBits(v)
	impl fun doubleToLongBits(v: Double): Long = java.lang.Double.doubleToLongBits(v)
	impl fun intBitsToFloat(v: Int): Float = java.lang.Float.intBitsToFloat(v)
	impl fun longBitsToDouble(v: Long): Double = java.lang.Double.longBitsToDouble(v)

	impl fun reverseBytes(v: Long): Long = java.lang.Long.reverseBytes(v)
	impl fun reverseBytes(v: Int): Int = java.lang.Integer.reverseBytes(v)
	impl fun reverseBytes(v: Short): Short = java.lang.Short.reverseBytes(v)
}