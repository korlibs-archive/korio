package com.soywiz.korio.lang

import com.soywiz.korio.ds.ByteArrayBuilder

abstract class Charset(val name: String) {
	abstract fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int = 0, end: Int = src.length)
	abstract fun decode(out: StringBuilder, src: ByteArray, start: Int = 0, end: Int = src.size)

	companion object {
		fun forName(name: String): Charset {
			return UTF8Charset
		}
	}
}

abstract class UTC8CharsetBase(name: String) : Charset(name) {
	private fun createByte(codePoint: Int, shift: Int): Int = codePoint shr shift and 0x3F or 0x80

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) {
			val codePoint = src[n].toInt()

			if (codePoint and 0x7F.inv() == 0) { // 1-byte sequence
				out.append(codePoint.toByte())
			} else {
				if (codePoint and 0x7FF.inv() == 0) { // 2-byte sequence
					out.append((codePoint shr 6 and 0x1F or 0xC0).toByte())
				} else if (codePoint and 0xFFFF.inv() == 0) { // 3-byte sequence
					out.append((codePoint shr 12 and 0x0F or 0xE0).toByte())
					out.append((createByte(codePoint, 6)).toByte())
				} else if (codePoint and -0x200000 == 0) { // 4-byte sequence
					out.append((codePoint shr 18 and 0x07 or 0xF0).toByte())
					out.append((createByte(codePoint, 12)).toByte())
					out.append((createByte(codePoint, 6)).toByte())
				}
				out.append((codePoint and 0x3F or 0x80).toByte())
			}
		}
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		var i = start
		while (i < end) {
			val c = src[i++].toInt() and 0xFF
			when (c shr 4) {
				0, 1, 2, 3, 4, 5, 6, 7 -> {
					// 0xxxxxxx
					out.append(c.toChar())
				}
				12, 13 -> {
					// 110x xxxx   10xx xxxx
					out.append((c and 0x1F shl 6 or (src[i++].toInt() and 0x3F)).toChar())
				}
				14 -> {
					// 1110 xxxx  10xx xxxx  10xx xxxx
					out.append((c and 0x0F shl 12 or (src[i++].toInt() and 0x3F shl 6) or (src[i++].toInt() and 0x3F)).toChar())
				}
			}
		}
	}
}

open class SingleByteCharset(name: String, val conv: String) : Charset(name) {
	// @TODO: Optimize this (IntIntMap)
	val v = conv.withIndex().map { it.value.toInt() to it.index }.toMap()

	override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
		for (n in start until end) {
			out.append(v[src[n].toInt()]?.toByte() ?: '?'.toByte())
		}
	}

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		for (n in start until end) {
			out.append(conv[src[n].toInt() and 0xFF])
		}
	}
}

object ISO_8859_1Charset : SingleByteCharset(
	"ISO-8859-1",
	"\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u0008\t\n\u000b\u000c\r\u000e\u000f\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001a\u001b\u001c\u001d\u001e\u001f !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007f\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008a\u008b\u008c\u008d\u008e\u008f\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009a\u009b\u009c\u009d\u009e\u009f\u00a0¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ"
)

object UTF8Charset : UTC8CharsetBase("UTF-8")

object Charsets {
	val UTF_8 = UTF8Charset
	val ISO_8859_1 = ISO_8859_1Charset
}

val UTF8 = UTF8Charset

fun String.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray {
	val out = ByteArrayBuilder()
	charset.encode(out, this)
	return out.toByteArray()
}

fun ByteArray.toString(charset: Charset): String {
	val out = StringBuilder()
	charset.decode(out, this)
	return out.toString()
}