@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korio.util

import com.soywiz.korio.crypto.Hex
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

inline fun Int.mask(): Int = (1 shl this) - 1
inline fun Long.mask(): Long = (1L shl this.toInt()) - 1L
fun Int.toUInt(): Long = this.toLong() and 0xFFFFFFFFL
fun Int.getBits(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
fun Int.extract8(offset: Int): Int = (this ushr offset) and 0xFF
fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 1) != 0

fun Int.extractScaled(offset: Int, count: Int, scale: Int): Int {
	val mask = count.mask()
	return (extract(offset, count) * scale) / mask
}

fun Int.extractScaledf01(offset: Int, count: Int): Double {
	val mask = count.mask().toDouble()
	return extract(offset, count).toDouble() / mask
}

fun Int.extractScaledFF(offset: Int, count: Int): Int = extractScaled(offset, count, 0xFF)
fun Int.extractScaledFFDefault(offset: Int, count: Int, default: Int): Int = if (count == 0) default else extractScaled(offset, count, 0xFF)

fun Int.insert(value: Int, offset: Int, count: Int): Int {
	val mask = count.mask()
	val clearValue = this and (mask shl offset).inv()
	return clearValue or ((value and mask) shl offset)
}

fun Int.insert8(value: Int, offset: Int): Int = insert(value, offset, 8)

fun Int.insert(value: Boolean, offset: Int): Int = this.insert(if (value) 1 else 0, offset, 1)

fun Int.insertScaled(value: Int, offset: Int, count: Int, scale: Int): Int {
	val mask = count.mask()
	return insert((value * mask) / scale, offset, count)
}

fun Int.insertScaledFF(value: Int, offset: Int, count: Int): Int = if (count == 0) this else this.insertScaled(value, offset, count, 0xFF)

fun Int.nextAlignedTo(align: Int) = if (this % align == 0) {
	this
} else {
	(((this / align) + 1) * align)
}

fun Long.nextAlignedTo(align: Long) = if (this % align == 0L) {
	this
} else {
	(((this / align) + 1) * align)
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

fun Byte.toUnsigned() = this.toInt() and 0xFF
fun Int.toUnsigned() = this.toLong() and 0xFFFFFFFFL

fun Int.signExtend(bits: Int) = (this shl (32 - bits)) shr (32 - bits)
fun Long.signExtend(bits: Int) = (this shl (64 - bits)) shr (64 - bits)

val Float.niceStr: String get() = if (this.toLong().toFloat() == this) "${this.toLong()}" else "$this"
val Double.niceStr: String get() = if (this.toLong().toDouble() == this) "${this.toLong()}" else "$this"

infix fun Int.umod(other: Int): Int {
	val remainder = this % other
	return when {
		remainder < 0 -> remainder + other
		else -> remainder
	}
}

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
	var temp = this
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp % radix
			temp /= radix
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		return out.reversed()
	}
}