package com.soywiz.korio.util

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
