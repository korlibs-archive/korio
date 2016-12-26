package com.soywiz.coktvfs.util

fun ByteArray.readS8(o: Int):Int = this[o].toInt()
fun ByteArray.readU8(o: Int):Int = this[o].toInt() and 0xFF

fun ByteArray.readS16_le(o: Int):Int = (((readU8(o + 0) shl 0) or (readU8(o + 1) shl 8)) shl 16) shr 16
fun ByteArray.readS32_le(o: Int):Int = ((readU8(o + 0) shl 0) or (readU8(o + 1) shl 8) or (readU8(o + 2) shl 16) or (readU8(o + 3) shl 24))
fun ByteArray.readU16_le(o: Int):Int = (readU8(o + 0) shl 0) or (readU8(o + 1) shl 8)
fun ByteArray.readU32_le(o: Int):Long = readS32_le(o).toLong() and 0xFFFFFFFFL
fun ByteArray.readS64_le(o: Int):Long = (readU32_le(o + 0) shl 0) or (readU32_le(o + 4) shl 32)

fun ByteArray.readF32_le(o: Int):Float = java.lang.Float.intBitsToFloat(readS32_le(o))
fun ByteArray.readF64_le(o: Int):Double = java.lang.Double.longBitsToDouble(readS64_le(o))

fun ByteArray.readS16_be(o: Int):Int = (((readU8(o + 0) shl 8) or (readU8(o + 1) shl 0)) shl 16) shr 16
fun ByteArray.readS32_be(o: Int):Int = ((readU8(o + 0) shl 24) or (readU8(o + 1) shl 16) or (readU8(o + 2) shl 8) or (readU8(o + 3) shl 0))
fun ByteArray.readU16_be(o: Int):Int = (readU8(o + 0) shl 8) or (readU8(o + 1) shl 0)
fun ByteArray.readU32_be(o: Int):Long = readS32_le(o).toLong() and 0xFFFFFFFFL
fun ByteArray.readS64_be(o: Int):Long = (readU32_le(o + 0) shl 32) or (readU32_le(o + 4) shl 0)

fun ByteArray.readF32_be(o: Int):Float = java.lang.Float.intBitsToFloat(readS32_be(o))
fun ByteArray.readF64_be(o: Int):Double = java.lang.Double.longBitsToDouble(readS64_be(o))
