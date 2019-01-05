package com.soywiz.korio.lang

import com.soywiz.kds.*
import com.soywiz.kmem.*

abstract class Charset(val name: String) {
	abstract fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int = 0, end: Int = src.length)
	abstract fun decode(out: StringBuilder, src: ByteArray, start: Int = 0, end: Int = src.size)

	companion object {
		fun forName(name: String): Charset {
			return UTF8
		}
	}
}

open class UTC8CharsetBase(name: String) : Charset(name) {
	private fun createByte(codePoint: Int, shift: Int): Int = codePoint shr shift and 0x3F or 0x80

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) {
			val codePoint = src[n].toInt()

			if (codePoint and 0x7F.inv() == 0) { // 1-byte sequence
				out.append(codePoint.toByte())
			} else {
				when {
					codePoint and 0x7FF.inv() == 0 -> // 2-byte sequence
						out.append((codePoint shr 6 and 0x1F or 0xC0).toByte())
					codePoint and 0xFFFF.inv() == 0 -> { // 3-byte sequence
						out.append((codePoint shr 12 and 0x0F or 0xE0).toByte())
						out.append((createByte(codePoint, 6)).toByte())
					}
					codePoint and -0x200000 == 0 -> { // 4-byte sequence
						out.append((codePoint shr 18 and 0x07 or 0xF0).toByte())
						out.append((createByte(codePoint, 12)).toByte())
						out.append((createByte(codePoint, 6)).toByte())
					}
				}
				out.append((codePoint and 0x3F or 0x80).toByte())
			}
		}
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		if ((start < 0 || start > src.size) || (end < 0 || end > src.size)) error("Out of bounds")
		var i = start
		while (i < end) {
			// @TODO: kotlin-js generates that looks pretty slow:
			// val c = src[i++].toInt() and 0xFF
			//   --->  var c = src[tmp$ = i, i = tmp$ + 1 | 0, tmp$] & 255;
			val c = src[i].toInt() and 0xFF
			when (c shr 4) {
				0, 1, 2, 3, 4, 5, 6, 7 -> {
					// 0xxxxxxx
					out.append(c.toChar())
					i += 1
				}
				12, 13 -> {
					// 110x xxxx   10xx xxxx
					out.append((c and 0x1F shl 6 or (src[i + 1].toInt() and 0x3F)).toChar())
					i += 2
				}
				14 -> {
					// 1110 xxxx  10xx xxxx  10xx xxxx
					out.append((c and 0x0F shl 12 or (src[i + 1].toInt() and 0x3F shl 6) or (src[i + 2].toInt() and 0x3F)).toChar())
					i += 3
				}
				else -> {
					i += 1
				}
			}
		}
	}
}

open class SingleByteCharset(name: String, val conv: String) : Charset(name) {
	val v: IntIntMap = IntIntMap().apply {
		for (n in 0 until conv.length) this[conv[n].toInt()] = n
	}

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) {
			val c = src[n].toInt()
			out.append(if (v.contains(c)) v[c].toByte() else '?'.toByte())
		}
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end) {
			out.append(conv[src[n].toInt() and 0xFF])
		}
	}
}

object ISO_8859_1 : SingleByteCharset("ISO-8859-1", buildString { for (n in 0 until 256) append(n.toChar()) })

expect val UTF8: Charset

class UTF16Charset(val le: Boolean) : Charset("UTF-16-" + (if (le) "LE" else "BE")) {
	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end step 2) out.append(src.readS16(n, le).toChar())
	}

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		val temp = ByteArray(2)
		for (n in start until end) {
			temp.write16(0, src[n].toInt(), le)
			out.append(temp)
		}
	}
}

object ASCII : Charset("ASCII") {
	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) out.append(src[n].toByte())
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end) out.append(src[n].toChar())
	}
}

val LATIN1 = ISO_8859_1
val UTF16_LE = UTF16Charset(le = true)
val UTF16_BE = UTF16Charset(le = false)

object Charsets {
	val UTF8 get() = com.soywiz.korio.lang.UTF8
	val LATIN1 get() = com.soywiz.korio.lang.LATIN1
	val UTF16_LE get() = com.soywiz.korio.lang.UTF16_LE
	val UTF16_BE get() = com.soywiz.korio.lang.UTF16_BE
}

fun String.toByteArray(charset: Charset = UTF8): ByteArray {
	val out = ByteArrayBuilder()
	charset.encode(out, this)
	return out.toByteArray()
}

fun ByteArray.toString(charset: Charset): String {
	val out = StringBuilder()
	charset.decode(out, this)
	return out.toString()
}

fun ByteArray.toUtf8String() = this.toString(UTF8)

fun ByteArray.readStringz(o: Int, size: Int, charset: Charset = UTF8): String {
	var idx = o
	val stop = kotlin.math.min(this.size, o + size)
	while (idx < stop) {
		if (this[idx] == 0.toByte()) break
		idx++
	}
	return this.copyOfRange(o, idx).toString(charset)
}

fun ByteArray.readStringz(o: Int, charset: Charset = UTF8): String {
	return readStringz(o, size - o, charset)
}

fun ByteArray.readString(o: Int, size: Int, charset: Charset = UTF8): String {
	return this.copyOfRange(o, o + size).toString(charset)
}
