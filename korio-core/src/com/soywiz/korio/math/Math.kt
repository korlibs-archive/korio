package com.soywiz.korio.math

header object Math {
	fun min(a: Int, b: Int): Int
	fun min(a: Long, b: Long): Long
	fun max(a: Int, b: Int): Int
	fun max(a: Long, b: Long): Long
	fun pow(d: Double, toDouble: Double): Double
	fun ceil(v: Double): Double
	fun floor(v: Double): Double
	fun round(v: Double): Double
}

object MathEx {
	fun floatToIntBits(item: Float): Int = TODO()
	fun doubleToLongBits(v: Double): Long = TODO()
	fun reverseBytes(v: Long): Long = TODO()
	fun reverseBytes(v: Int): Int = TODO()
	fun reverseBytes(v: Short): Short = TODO()
}