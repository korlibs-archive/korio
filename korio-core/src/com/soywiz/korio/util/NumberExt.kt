package com.soywiz.korio.util

fun Int.toUInt(): Long = this.toLong() and 0xFFFFFFFFL
fun Int.getBits(offset: Int, count: Int): Int = (this ushr offset) and ((1 shl count) - 1)
fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and ((1 shl count) - 1)
fun Int.extract8(offset: Int): Int = this.extract(offset, 8)
fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 1) != 0

fun Int.insert(value: Int, offset: Int, count: Int): Int {
	val mask = (1 shl count) - 1
	val clearValue = this and (mask shl offset).inv()
	return clearValue or ((value and mask) shl offset)
}

fun Int.insert(value: Boolean, offset: Int): Int = this.insert(if (value) 1 else 0, offset, 1)

fun Long.nextAlignedTo(align: Long) = if (this % align == 0L) {
	this
} else {
	(((this / align) + 1) * align)
}

fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
fun Long.clamp(min: Long, max: Long): Long = if (this < min) min else if (this > max) max else this

fun Long.toIntSafe(): Int {
	if (this.toInt().toLong() != this) throw IllegalArgumentException("Long doesn't fit Integer")
	return this.toInt()
}
