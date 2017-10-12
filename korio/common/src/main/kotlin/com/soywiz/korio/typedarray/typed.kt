package com.soywiz.korio.typedarray

import com.soywiz.korio.KorioNative

fun <T> Array<T>.copyRangeTo(srcPos: Int, dst: Array<T>, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)
fun BooleanArray.copyRangeTo(srcPos: Int, dst: BooleanArray, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)
fun ByteArray.copyRangeTo(srcPos: Int, dst: ByteArray, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)
fun ShortArray.copyRangeTo(srcPos: Int, dst: ShortArray, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)
fun IntArray.copyRangeTo(srcPos: Int, dst: IntArray, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)
fun LongArray.copyRangeTo(srcPos: Int, dst: LongArray, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)
fun FloatArray.copyRangeTo(srcPos: Int, dst: FloatArray, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)
fun DoubleArray.copyRangeTo(srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int): Unit = KorioNative.copyRangeTo(this, srcPos, dst, dstPos, count)

fun <T> Array<T>.fill(value: T, from: Int = 0, to: Int = this.size): Unit = KorioNative.fill(this, value, from, to)
fun BooleanArray.fill(value: Boolean, from: Int = 0, to: Int = this.size): Unit = KorioNative.fill(this, value, from, to)
fun ByteArray.fill(value: Byte, from: Int = 0, to: Int = this.size): Unit = KorioNative.fill(this, value, from, to)
fun ShortArray.fill(value: Short, from: Int = 0, to: Int = this.size): Unit = KorioNative.fill(this, value, from, to)
fun IntArray.fill(value: Int, from: Int = 0, to: Int = this.size): Unit = KorioNative.fill(this, value, from, to)
fun FloatArray.fill(value: Float, from: Int = 0, to: Int = this.size): Unit = KorioNative.fill(this, value, from, to)
fun DoubleArray.fill(value: Double, from: Int = 0, to: Int = this.size): Unit = KorioNative.fill(this, value, from, to)
