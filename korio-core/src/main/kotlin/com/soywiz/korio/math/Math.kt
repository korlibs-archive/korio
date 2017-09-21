package com.soywiz.korio.math

header object Math {
	fun min(a: Int, b: Int): Int
	fun min(a: Long, b: Long): Long
	fun max(a: Int, b: Int): Int
	fun max(a: Long, b: Long): Long
	fun pow(a: Double, b: Double): Double
	fun ceil(v: Double): Double
	fun floor(v: Double): Double
	fun round(v: Double): Long
}

header object MathEx {
	fun floatToIntBits(v: Float): Int
	fun doubleToLongBits(v: Double): Long
	fun intBitsToFloat(v: Int): Float
	fun longBitsToDouble(v: Long): Double

	fun reverseBytes(v: Long): Long
	fun reverseBytes(v: Int): Int
	fun reverseBytes(v: Short): Short
}