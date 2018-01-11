@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korio.util

import com.soywiz.kmem.*
import com.soywiz.korio.crypto.Hex
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

fun Int.nextAlignedTo(align: Int) = when {
	align == 0 -> this
	(this % align) == 0 -> this
	else -> (((this / align) + 1) * align)
}

fun Long.nextAlignedTo(align: Long) = when {
	align == 0L -> this
	(this % align) == 0L -> this
	else -> (((this / align) + 1) * align)
}

fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
fun Double.clamp(min: Double, max: Double): Double = if (this < min) min else if (this > max) max else this
fun Long.clamp(min: Long, max: Long): Long = if (this < min) min else if (this > max) max else this

fun Long.toIntSafe(): Int {
	if (this.toInt().toLong() != this) throw IllegalArgumentException("Long doesn't fit Integer")
	return this.toInt()
}

fun Long.toIntClamp(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
	if (this < min) return min
	if (this > max) return max
	return this.toInt()
}

fun Long.toUintClamp(min: Int = 0, max: Int = Int.MAX_VALUE) = this.toIntClamp(0, Int.MAX_VALUE)

fun String.toNumber(): Number = this.toIntOrNull() as Number? ?: this.toLongOrNull() as Number? ?: this.toDoubleOrNull() as Number? ?: 0

val Float.niceStr: String get() = if (this.toLong().toFloat() == this) "${this.toLong()}" else "$this"
val Double.niceStr: String get() = if (this.toLong().toDouble() == this) "${this.toLong()}" else "$this"

fun Double.convertRange(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double {
	val ratio = (this - srcMin) / (srcMax - srcMin)
	return (dstMin + (dstMax - dstMin) * ratio)
}

fun Double.convertRangeClamped(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)

fun Long.convertRange(srcMin: Long, srcMax: Long, dstMin: Long, dstMax: Long): Long {
	val ratio = (this - srcMin).toDouble() / (srcMax - srcMin).toDouble()
	return (dstMin + (dstMax - dstMin) * ratio).toLong()
}

fun Double.toIntCeil() = ceil(this).toInt()
fun Double.toIntFloor() = floor(this).toInt()
fun Double.toIntRound() = round(this).toLong().toInt()

val Int.isOdd get() = (this % 2) == 1
val Int.isEven get() = (this % 2) == 0

fun Long.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp % radix
			temp /= radix
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return if (isNegative) "-$rout" else rout
	}
}

fun Int.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0) {
		return "0"
	} else {
		var out = ""
		while (temp != 0) {
			val digit = temp % radix
			temp /= radix
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return if (isNegative) "-$rout" else rout
	}
}

fun Int.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0) {
		return "0"
	} else {
		var out = ""
		while (temp != 0) {
			val digit = temp urem  radix
			temp = temp udiv radix
			out += Hex.DIGITS_UPPER[digit]
		}
		val rout = out.reversed()
		return rout
	}
}

fun Long.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp urem radix.toLong()
			temp = temp udiv radix.toLong()
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return rout
	}
}

object LongEx {
	val MIN_VALUE: Long = 0x7fffffffffffffffL.inv()
	val MAX_VALUE: Long = 0x7fffffffffffffffL

	fun compare(x: Long, y: Long): Int = if (x < y) -1 else if (x == y) 0 else 1
	fun compareUnsigned(x: Long, y: Long): Int = compare(x xor MIN_VALUE, y xor MIN_VALUE)

	fun divideUnsigned(dividend: Long, divisor: Long): Long {
		if (divisor < 0) return (if (compareUnsigned(dividend, divisor) < 0) 0 else 1).toLong()
		if (dividend >= 0) return dividend / divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return quotient + if (compareUnsigned(rem, divisor) >= 0) 1 else 0
	}

	fun remainderUnsigned(dividend: Long, divisor: Long): Long {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) dividend else dividend - divisor
		if (dividend >= 0) return dividend % divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return rem - if (compareUnsigned(rem, divisor) >= 0) divisor else 0
	}
}

object IntEx {
	private val MIN_VALUE = -0x80000000
	private val MAX_VALUE = 0x7fffffff

	fun compare(l: Int, r: Int): Int = if (l < r) -1 else if (l > r) 1 else 0
	fun compareUnsigned(l: Int, r: Int): Int = compare(l xor MIN_VALUE, r xor MIN_VALUE)
	fun divideUnsigned(dividend: Int, divisor: Int): Int {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) 0 else 1
		if (dividend >= 0) return dividend / divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return quotient + if (compareUnsigned(rem, divisor) >= 0) 1 else 0
	}

	fun remainderUnsigned(dividend: Int, divisor: Int): Int {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) dividend else dividend - divisor
		if (dividend >= 0) return dividend % divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return rem - if (compareUnsigned(rem, divisor) >= 0) divisor else 0
	}
}

infix fun Int.udiv(that: Int) = IntEx.divideUnsigned(this, that)
infix fun Int.urem(that: Int) = IntEx.remainderUnsigned(this, that)

infix fun Long.udiv(that: Long) = LongEx.divideUnsigned(this, that)
infix fun Long.urem(that: Long) = LongEx.remainderUnsigned(this, that)

fun Float.clamp(min: Float, max: Float) = when {
	(this < min) -> min
	(this > max) -> max
	else -> this
}

fun Float.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun imul32_64(a: Int, b: Int, result: IntArray = IntArray(2)): IntArray {
	if (a == 0) {
		result[0] = 0
		result[1] = 0
		return result
	}
	if (b == 0) {
		result[0] = 0
		result[1] = 0
		return result
	}

	if ((a >= -32768 && a <= 32767) && (b >= -32768 && b <= 32767)) {
		result[0] = a * b
		result[1] = if (result[0] < 0) -1 else 0
		return result
	}

	val doNegate = (a < 0) xor (b < 0)

	umul32_64(abs(a), abs(b), result)

	if (doNegate) {
		result[0] = result[0].inv()
		result[1] = result[1].inv()
		result[0] = (result[0] + 1) or 0
		if (result[0] == 0) result[1] = (result[1] + 1) or 0
	}

	return result
}

fun umul32_64(a: Int, b: Int, result: IntArray = IntArray(2)): IntArray {
	if (a ult 32767 && b ult 65536) {
		result[0] = a * b
		result[1] = if (result[0] < 0) -1 else 0
		return result
	}

	val a00 = a and 0xFFFF
	val a16 = a ushr 16
	val b00 = b and 0xFFFF
	val b16 = b ushr 16
	val c00 = a00 * b00
	var c16 = (c00 ushr 16) + (a16 * b00)
	var c32 = c16 ushr 16
	c16 = (c16 and 0xFFFF) + (a00 * b16)
	c32 += c16 ushr 16
	var c48 = c32 ushr 16
	c32 = (c32 and 0xFFFF) + (a16 * b16)
	c48 += c32 ushr 16

	result[0] = ((c16 and 0xFFFF) shl 16) or (c00 and 0xFFFF)
	result[1] = ((c48 and 0xFFFF) shl 16) or (c32 and 0xFFFF)
	return result
}

val Int.unsigned: Long get() = this.toLong() and 0xFFFFFFFF

object BitUtils {
	fun mask(value: Int): Int = value.mask()
	fun bitrev32(x: Int): Int = x.reverseBits()
	fun rotr(value: Int, offset: Int): Int = value.rotateRight(offset)
	fun clz32(x: Int): Int = x.countLeadingZeros()
	fun clo(x: Int): Int = clz32(x.inv())
	fun clz(x: Int): Int = clz32(x)
	fun seb(x: Int): Int = (x shl 24) shr 24
	fun seh(x: Int): Int = (x shl 16) shr 16
	fun wsbh(v: Int): Int = ((v and 0xFF00FF00.toInt()) ushr 8) or ((v and 0x00FF00FF) shl 8)
	fun wsbw(v: Int): Int = (
		((v and 0xFF000000.toInt()) ushr 24) or
			((v and 0x00FF0000) ushr 8) or
			((v and 0x0000FF00) shl 8) or
			((v and 0x000000FF) shl 24)
		)
}

fun Int.compareToUnsigned(that: Int) = IntEx.compareUnsigned(this, that)

// l xor MIN_VALUE, r xor MIN_VALUE

//const val INT_MIN_VALUE = -0x80000000
//const val INT_MAX_VALUE = 0x7fffffff

infix inline fun Int.ult(that: Int) = (this xor (-0x80000000)) < (that xor (-0x80000000))

//infix fun Int.ult(that: Int) = IntEx.compareUnsigned(this, that) < 0
infix fun Int.ule(that: Int) = IntEx.compareUnsigned(this, that) <= 0
infix fun Int.ugt(that: Int) = IntEx.compareUnsigned(this, that) > 0
infix fun Int.uge(that: Int) = IntEx.compareUnsigned(this, that) >= 0

// @TODO: Move to Kmem
fun Int.extractBool(offset: Int) = this.extract(offset, 1) != 0

// @TODO: Move to Kmem
infix fun Int.hasFlag(bits: Int) = (this and bits) == bits
