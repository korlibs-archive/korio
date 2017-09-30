package com.soywiz.korio.lang

import com.soywiz.korio.typedarray.Arrays

object System {
	@Deprecated("", ReplaceWith("Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)", "com.soywiz.korio.typedarray.Arrays"))
	fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) = Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)

	@Deprecated("", ReplaceWith("Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)", "com.soywiz.korio.typedarray.Arrays"))
	fun arraycopy(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int) = Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)

	@Deprecated("", ReplaceWith("Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)", "com.soywiz.korio.typedarray.Arrays"))
	fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) = Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)

	@Deprecated("", ReplaceWith("Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)", "com.soywiz.korio.typedarray.Arrays"))
	fun arraycopy(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int) = Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)

	@Deprecated("", ReplaceWith("Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)", "com.soywiz.korio.typedarray.Arrays"))
	fun arraycopy(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int) = Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)

	@Deprecated("", ReplaceWith("Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)", "com.soywiz.korio.typedarray.Arrays"))
	fun arraycopy(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int) = Arrays.copyRangeTo(src, srcPos, dst, dstPos, count)
}

inline fun assert(cond: Boolean): Unit {
	if (!cond) throw AssertionError()
}