package com.soywiz.korio.typedarray

expect object Arrays {
	fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int)
	fun fill(src: BooleanArray, value: Boolean, from: Int, to: Int)
	fun fill(src: ByteArray, value: Byte, from: Int, to: Int)
	fun fill(src: ShortArray, value: Short, from: Int, to: Int)
	fun fill(src: IntArray, value: Int, from: Int, to: Int)
	fun fill(src: FloatArray, value: Float, from: Int, to: Int)
	fun fill(src: DoubleArray, value: Double, from: Int, to: Int)
}
