package com.soywiz.korio.util

fun ByteArray.readS8(o: Int): Int = this[o].toInt()
fun ByteArray.readU8(o: Int): Int = this[o].toInt() and 0xFF

fun ByteArray.readS16_le(o: Int): Int = (((readU8(o + 0) shl 0) or (readU8(o + 1) shl 8)) shl 16) shr 16
fun ByteArray.readS32_le(o: Int): Int = ((readU8(o + 0) shl 0) or (readU8(o + 1) shl 8) or (readU8(o + 2) shl 16) or (readU8(o + 3) shl 24))
fun ByteArray.readU16_le(o: Int): Int = (readU8(o + 0) shl 0) or (readU8(o + 1) shl 8)
fun ByteArray.readU24_le(o: Int): Int = (readU8(o + 0) shl 0) or (readU8(o + 1) shl 8) or (readU8(o + 2) shl 16)
fun ByteArray.readU32_le(o: Int): Long = readS32_le(o).toLong() and 0xFFFFFFFFL
fun ByteArray.readS64_le(o: Int): Long = (readU32_le(o + 0) shl 0) or (readU32_le(o + 4) shl 32)

fun ByteArray.readF32_le(o: Int): Float = java.lang.Float.intBitsToFloat(readS32_le(o))
fun ByteArray.readF64_le(o: Int): Double = java.lang.Double.longBitsToDouble(readS64_le(o))

fun ByteArray.readS16_be(o: Int): Int = (((readU8(o + 0) shl 8) or (readU8(o + 1) shl 0)) shl 16) shr 16
fun ByteArray.readS32_be(o: Int): Int = ((readU8(o + 0) shl 24) or (readU8(o + 1) shl 16) or (readU8(o + 2) shl 8) or (readU8(o + 3) shl 0))
fun ByteArray.readU16_be(o: Int): Int = (readU8(o + 0) shl 8) or (readU8(o + 1) shl 0)
fun ByteArray.readU24_be(o: Int): Int = (readU8(o + 0) shl 16) or (readU8(o + 1) shl 8) or (readU8(o + 2) shl 0)
fun ByteArray.readU32_be(o: Int): Long = readS32_be(o).toLong() and 0xFFFFFFFFL
fun ByteArray.readS64_be(o: Int): Long = (readU32_be(o + 0) shl 32) or (readU32_be(o + 4) shl 0)

fun ByteArray.readF32_be(o: Int): Float = java.lang.Float.intBitsToFloat(readS32_be(o))
fun ByteArray.readF64_be(o: Int): Double = java.lang.Double.longBitsToDouble(readS64_be(o))

private inline fun <T> ByteArray.readTypedArray(o: Int, count: Int, elementSize: Int, crossinline gen: () -> T, crossinline read: ByteArray.(array: T, n: Int, pos: Int) -> Unit): T {
	val array = gen()
	var pos = o
	for (n in 0 until count) {
		read(this, array, n, pos)
		pos += elementSize
	}
	return array
}

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
