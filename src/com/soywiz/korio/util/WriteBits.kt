package com.soywiz.korio.util

fun ByteArray.write8(o: Int, v: Int) = run { this[o] = (v and 0xFF).toByte() }
fun ByteArray.write8(o: Int, v: Long) = run { this[o] = (v and 0xFF).toByte() }

fun ByteArray.write16_le(o: Int, v: Int) = run { write8(o + 0, v ushr 0); write8(o + 1, v ushr 8) }
fun ByteArray.write32_le(o: Int, v: Int) = run { write8(o + 0, v ushr 0); write8(o + 1, v ushr 8); write8(o + 2, v ushr 16); write8(o + 3, v ushr 24) }
fun ByteArray.write32_le(o: Int, v: Long) = write32_le(o, v.toInt())
fun ByteArray.write64_le(o: Int, v: Long) = run { write32_le(o + 0, (v ushr 0).toInt()); write32_le(o + 4, (v ushr 32).toInt()) }

fun ByteArray.writeF32_le(o: Int, v: Float) = run { write32_le(o + 0, java.lang.Float.floatToIntBits(v)) }
fun ByteArray.writeF64_le(o: Int, v: Double) = run { write64_le(o + 0, java.lang.Double.doubleToLongBits(v)) }

fun ByteArray.write16_be(o: Int, v: Int) = run { write8(o + 0, v ushr 8); write8(o + 1, v ushr 0) }
fun ByteArray.write32_be(o: Int, v: Int) = run { write8(o + 0, v ushr 24); write8(o + 1, v ushr 16); write8(o + 2, v ushr 8); write8(o + 3, v ushr 0) }
fun ByteArray.write32_be(o: Int, v: Long) = write32_be(o, v.toInt())
fun ByteArray.write64_be(o: Int, v: Long) = run { write32_le(o + 0, (v ushr 32).toInt()); write32_le(o + 4, (v ushr 0).toInt()) }

fun ByteArray.writeF32_be(o: Int, v: Float) = run { write32_be(o + 0, java.lang.Float.floatToIntBits(v)) }
fun ByteArray.writeF64_be(o: Int, v: Double) = run { write64_be(o + 0, java.lang.Double.doubleToLongBits(v)) }
