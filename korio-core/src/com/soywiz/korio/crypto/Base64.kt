package com.soywiz.korio.crypto

import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.util.UByteArray
import com.soywiz.korio.util.readU24_be
import com.soywiz.korio.util.readU8

object Base64 {
	private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
	private val DECODE by lazy {
		val out = IntArray(0x100)
		for (n in 0..255) out[n] = -1
		for (n in 0 until TABLE.length) {
			out[TABLE[n].toInt()] = n
		}
		out
	}

	fun decode(str: String): ByteArray {
		val src = str.toByteArray(Charsets.UTF_8)
		val dst = ByteArray(src.size)
		return dst.copyOf(decode(src, dst))
	}

	fun decode(src: ByteArray, dst: ByteArray): Int {
		var m = 0
		val srcu = UByteArray(src)

		var n = 0
		while (n < src.size) {
			val d = DECODE[srcu[n]]
			if (d < 0) {
				n++
				continue // skip character
			}

			val b0 = DECODE[srcu[n++]]
			val b1 = DECODE[srcu[n++]]
			val b2 = DECODE[srcu[n++]]
			val b3 = DECODE[srcu[n++]]
			dst[m++] = (b0 shl 2 or (b1 shr 4)).toByte()
			if (b2 < 64) {
				dst[m++] = (b1 shl 4 or (b2 shr 2)).toByte()
				if (b3 < 64) {
					dst[m++] = (b2 shl 6 or b3).toByte()
				}
			}
		}
		return m
	}

	fun encode(src: String, charset: Charset): String = encode(src, Charsets.UTF_8)

	@Suppress("UNUSED_CHANGED_VALUE")
	fun encode(src: ByteArray): String {
		val out = CharArray((src.size * 4) / 3 + 4)
		var opos = 0
		var ipos = 0
		val extraBytes = src.size % 3
		while (ipos < src.size - 2) {
			val num = src.readU24_be(ipos)
			ipos += 3

			out[opos++] = TABLE[(num ushr 18) and 0x3F]
			out[opos++] = TABLE[(num ushr 12) and 0x3F]
			out[opos++] = TABLE[(num ushr 6) and 0x3F]
			out[opos++] = TABLE[(num ushr 0) and 0x3F]
		}

		if (extraBytes == 1) {
			val num = src.readU8(ipos++)
			out[opos++] = TABLE[num ushr 2]
			out[opos++] = TABLE[(num shl 4) and 0x3F]
			out[opos++] = '='
			out[opos++] = '='
		} else if (extraBytes == 2) {
			val tmp = (src.readU8(ipos++) shl 8) or src.readU8(ipos++)
			out[opos++] = TABLE[tmp ushr 10]
			out[opos++] = TABLE[(tmp ushr 4) and 0x3F]
			out[opos++] = TABLE[(tmp shl 2) and 0x3F]
			out[opos++] = '='
		}

		return out.copyOf(opos).toString()
	}
}

fun String.fromBase64() = Base64.decode(this)
fun ByteArray.toBase64() = Base64.encode(this)