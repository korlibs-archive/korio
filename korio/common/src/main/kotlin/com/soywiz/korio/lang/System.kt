package com.soywiz.korio.lang

import com.soywiz.korio.typedarray.copyRangeTo

object System {
	fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) = src.copyRangeTo(srcPos, dst, dstPos, count)
	fun arraycopy(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int) = src.copyRangeTo(srcPos, dst, dstPos, count)
	//fun arraycopy(src: CharArray, srcPos: Int, dst: CharArray, dstPos: Int, count: Int) = src.copyRangeTo(srcPos, dst, dstPos, count)
	fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) = src.copyRangeTo(srcPos, dst, dstPos, count)
	fun arraycopy(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int) = src.copyRangeTo(srcPos, dst, dstPos, count)
}

infix fun Byte.and(mask: Long): Long = this.toLong() and mask

infix fun Byte.and(mask: Int): Int = this.toInt() and mask
infix fun Short.and(mask: Int): Int = this.toInt() and mask

infix fun Byte.or(mask: Int): Int = this.toInt() or mask
infix fun Short.or(mask: Int): Int = this.toInt() or mask
infix fun Short.or(mask: Short): Int = this.toInt() or mask.toInt()

infix fun Byte.shl(that: Int): Int = this.toInt() shl that
infix fun Short.shl(that: Int): Int = this.toInt() shl that

infix fun Byte.shr(that: Int): Int = this.toInt() shr that
infix fun Short.shr(that: Int): Int = this.toInt() shr that

infix fun Byte.ushr(that: Int): Int = this.toInt() ushr that
infix fun Short.ushr(that: Int): Int = this.toInt() ushr that
