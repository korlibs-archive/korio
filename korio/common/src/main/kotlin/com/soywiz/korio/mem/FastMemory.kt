package com.soywiz.korio.mem

import com.soywiz.korio.KorioNative

typealias FastMemory = KorioNative.FastMemory

fun FastMemory.getBytes(offset: Int, len: Int): ByteArray = ByteArray(len).apply { this@getBytes.getArrayInt8(offset, this@apply, 0, len) }
fun FastMemory.getShorts(offset: Int, len: Int): ShortArray = ShortArray(len).apply { this@getShorts.getAlignedArrayInt16(offset, this@apply, 0, len) }
fun FastMemory.getInts(offset: Int, len: Int): IntArray = IntArray(len).apply { this@getInts.getAlignedArrayInt32(offset, this@apply, 0, len) }
fun FastMemory.getFloats(offset: Int, len: Int): FloatArray = FloatArray(len).apply { this@getFloats.getAlignedArrayFloat32(offset, this@apply, 0, len) }