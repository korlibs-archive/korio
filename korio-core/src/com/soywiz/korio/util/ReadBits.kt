@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korio.util

import java.nio.charset.Charset
import java.util.*

private inline fun ByteArray._read8(o: Int): Int = this[o].toInt()

private inline fun ByteArray._read16_le(o: Int): Int = (readU8(o + 0) shl 0) or (readU8(o + 1) shl 8)
private inline fun ByteArray._read24_le(o: Int): Int = (readU8(o + 0) shl 0) or (readU8(o + 1) shl 8) or (readU8(o + 2) shl 16)
private inline fun ByteArray._read32_le(o: Int): Int = (readU8(o + 0) shl 0) or (readU8(o + 1) shl 8) or (readU8(o + 2) shl 16) or (readU8(o + 3) shl 24)
private inline fun ByteArray._read64_le(o: Int): Long = (_read32_le(o + 0).toUnsigned() shl 0) or (_read32_le(o + 4).toUnsigned() shl 32)

private inline fun ByteArray._read16_be(o: Int): Int = (readU8(o + 1) shl 0) or (readU8(o + 0) shl 8)
private inline fun ByteArray._read24_be(o: Int): Int = (readU8(o + 2) shl 0) or (readU8(o + 1) shl 8) or (readU8(o + 0) shl 16)
private inline fun ByteArray._read32_be(o: Int): Int = (readU8(o + 3) shl 0) or (readU8(o + 2) shl 8) or (readU8(o + 1) shl 16) or (readU8(o + 0) shl 24)
private inline fun ByteArray._read64_be(o: Int): Long = (_read32_be(o + 4).toUnsigned() shl 0) or (_read32_be(o + 0).toUnsigned() shl 32)

// Unsigned

fun ByteArray.readU8(o: Int): Int = this[o].toInt() and 0xFF
// LE
fun ByteArray.readU16_le(o: Int): Int = _read16_le(o)

fun ByteArray.readU24_le(o: Int): Int = _read24_le(o)
fun ByteArray.readU32_le(o: Int): Long = _read32_le(o).toUnsigned()
// BE
fun ByteArray.readU16_be(o: Int): Int = _read16_be(o)

fun ByteArray.readU24_be(o: Int): Int = _read24_be(o)
fun ByteArray.readU32_be(o: Int): Long = _read32_be(o).toUnsigned()

// Signed

fun ByteArray.readS8(o: Int): Int = this[o].toInt()
// LE
fun ByteArray.readS16_le(o: Int): Int = _read16_le(o).signExtend(16)

fun ByteArray.readS24_le(o: Int): Int = _read24_le(o).signExtend(24)
fun ByteArray.readS32_le(o: Int): Int = _read32_le(o)
fun ByteArray.readS64_le(o: Int): Long = _read64_le(o)
fun ByteArray.readF32_le(o: Int): Float = java.lang.Float.intBitsToFloat(_read32_le(o))
fun ByteArray.readF64_le(o: Int): Double = java.lang.Double.longBitsToDouble(_read64_le(o))
// BE
fun ByteArray.readS16_be(o: Int): Int = _read16_be(o).signExtend(16)

fun ByteArray.readS24_be(o: Int): Int = _read24_be(o).signExtend(24)
fun ByteArray.readS32_be(o: Int): Int = _read32_be(o)
fun ByteArray.readS64_be(o: Int): Long = _read64_be(o)
fun ByteArray.readF32_be(o: Int): Float = java.lang.Float.intBitsToFloat(_read32_be(o))
fun ByteArray.readF64_be(o: Int): Double = java.lang.Double.longBitsToDouble(_read64_be(o))

fun ByteArray.readS16_LEBE(o: Int, little: Boolean): Int = if (little) readS16_le(o) else readS16_be(o)
fun ByteArray.readS32_LEBE(o: Int, little: Boolean): Int = if (little) readS32_le(o) else readS32_be(o)
fun ByteArray.readS64_LEBE(o: Int, little: Boolean): Long = if (little) readS64_le(o) else readS64_be(o)
fun ByteArray.readF32_LEBE(o: Int, little: Boolean): Float = if (little) readF32_le(o) else readF32_be(o)
fun ByteArray.readF64_LEBE(o: Int, little: Boolean): Double = if (little) readF64_le(o) else readF64_be(o)

private inline fun <T> ByteArray.readTypedArray(o: Int, count: Int, elementSize: Int, crossinline gen: () -> T, crossinline read: ByteArray.(array: T, n: Int, pos: Int) -> Unit): T {
	val array = gen()
	var pos = o
	for (n in 0 until count) {
		read(this, array, n, pos)
		pos += elementSize
	}
	return array
}

fun ByteArray.readByteArray(o: Int, count: Int): ByteArray = Arrays.copyOfRange(this, o, o + count)

fun ByteArray.readShortArray_le(o: Int, count: Int): ShortArray = this.readTypedArray(o, count, 2, { ShortArray(count) }, { array, n, pos -> array[n] = readS16_le(pos).toShort() })
fun ByteArray.readCharArray_le(o: Int, count: Int): CharArray = this.readTypedArray(o, count, 2, { kotlin.CharArray(count) }, { array, n, pos -> array[n] = readS16_le(pos).toChar() })
fun ByteArray.readIntArray_le(o: Int, count: Int): IntArray = this.readTypedArray(o, count, 4, { IntArray(count) }, { array, n, pos -> array[n] = readS32_le(pos) })
fun ByteArray.readLongArray_le(o: Int, count: Int): LongArray = this.readTypedArray(o, count, 8, { LongArray(count) }, { array, n, pos -> array[n] = readS64_le(pos) })
fun ByteArray.readFloatArray_le(o: Int, count: Int): FloatArray = this.readTypedArray(o, count, 4, { FloatArray(count) }, { array, n, pos -> array[n] = readF32_le(pos) })
fun ByteArray.readDoubleArray_le(o: Int, count: Int): DoubleArray = this.readTypedArray(o, count, 8, { DoubleArray(count) }, { array, n, pos -> array[n] = readF64_le(pos) })

fun ByteArray.readShortArray_be(o: Int, count: Int): ShortArray = this.readTypedArray(o, count, 2, { ShortArray(count) }, { array, n, pos -> array[n] = readS16_be(pos).toShort() })
fun ByteArray.readCharArray_be(o: Int, count: Int): CharArray = this.readTypedArray(o, count, 2, { kotlin.CharArray(count) }, { array, n, pos -> array[n] = readS16_be(pos).toChar() })
fun ByteArray.readIntArray_be(o: Int, count: Int): IntArray = this.readTypedArray(o, count, 4, { IntArray(count) }, { array, n, pos -> array[n] = readS32_be(pos) })
fun ByteArray.readLongArray_be(o: Int, count: Int): LongArray = this.readTypedArray(o, count, 8, { LongArray(count) }, { array, n, pos -> array[n] = readS64_be(pos) })
fun ByteArray.readFloatArray_be(o: Int, count: Int): FloatArray = this.readTypedArray(o, count, 4, { FloatArray(count) }, { array, n, pos -> array[n] = readF32_be(pos) })
fun ByteArray.readDoubleArray_be(o: Int, count: Int): DoubleArray = this.readTypedArray(o, count, 8, { DoubleArray(count) }, { array, n, pos -> array[n] = readF64_be(pos) })

fun ByteArray.readStringz(o: Int, size: Int, charset: Charset = Charsets.UTF_8): String {
	var idx = o
	val stop = Math.min(this.size, o + size)
	while (idx < stop) {
		if (this[idx] == 0.toByte()) break
		idx++
	}
	return Arrays.copyOfRange(this, o, idx).toString(charset)
}

fun ByteArray.readStringz(o: Int, charset: Charset = Charsets.UTF_8): String {
	return readStringz(o, size - o, charset)
}