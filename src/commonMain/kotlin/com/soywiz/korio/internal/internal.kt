package com.soywiz.korio.internal

internal fun ByteArray.contains(other: ByteArray): Boolean = indexOf(other) >= 0

internal fun ByteArray.indexOf(other: ByteArray): Int {
	val full = this
	for (n in 0 until full.size - other.size) if (other.indices.all { full[n + it] == other[it] }) return n
	return -1
}


internal infix fun Byte.and(mask: Long): Long = this.toLong() and mask
internal infix fun Byte.and(mask: Int): Int = this.toInt() and mask
internal infix fun Short.and(mask: Int): Int = this.toInt() and mask

//internal infix fun Byte.or(mask: Int): Int = this.toInt() or mask
//internal infix fun Short.or(mask: Int): Int = this.toInt() or mask
//internal infix fun Short.or(mask: Short): Int = this.toInt() or mask.toInt()

//internal infix fun Byte.xor(mask: Int): Int = this.toInt() xor mask
//internal infix fun Short.xor(mask: Int): Int = this.toInt() xor mask
//internal infix fun Short.xor(mask: Short): Int = this.toInt() xor mask.toInt()

internal infix fun Byte.shl(that: Int): Int = this.toInt() shl that
internal infix fun Short.shl(that: Int): Int = this.toInt() shl that

internal infix fun Byte.shr(that: Int): Int = this.toInt() shr that
internal infix fun Short.shr(that: Int): Int = this.toInt() shr that

internal infix fun Byte.ushr(that: Int): Int = this.toInt() ushr that
internal infix fun Short.ushr(that: Int): Int = this.toInt() ushr that
