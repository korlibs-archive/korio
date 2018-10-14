package com.soywiz.korio.crypto

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*

object Base64 {
	private val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
	private val DECODE = IntArray(0x100).apply {
		for (n in 0..255) this[n] = -1
		for (n in 0 until TABLE.length) {
			this[TABLE[n].toInt()] = n
		}
	}

	fun decode(str: String): ByteArray {
		val src = str.toByteArray(UTF8)
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

	fun encode(src: String, charset: Charset): String = encode(src.toByteArray(charset))

	@Suppress("UNUSED_CHANGED_VALUE")
	fun encode(src: ByteArray): String {
		val out = StringBuilder((src.size * 4) / 3 + 4)
		var ipos = 0
		val extraBytes = src.size % 3
		while (ipos < src.size - 2) {
			val num = src.readU24_be(ipos)
			ipos += 3

			out.append(TABLE[(num ushr 18) and 0x3F])
			out.append(TABLE[(num ushr 12) and 0x3F])
			out.append(TABLE[(num ushr 6) and 0x3F])
			out.append(TABLE[(num ushr 0) and 0x3F])
		}

		if (extraBytes == 1) {
			val num = src.readU8(ipos++)
			out.append(TABLE[num ushr 2])
			out.append(TABLE[(num shl 4) and 0x3F])
			out.append('=')
			out.append('=')
		} else if (extraBytes == 2) {
			val tmp = (src.readU8(ipos++) shl 8) or src.readU8(ipos++)
			out.append(TABLE[tmp ushr 10])
			out.append(TABLE[(tmp ushr 4) and 0x3F])
			out.append(TABLE[(tmp shl 2) and 0x3F])
			out.append('=')
		}

		return out.toString()
	}
}

fun String.fromBase64() = Base64.decode(this)
fun ByteArray.toBase64() = Base64.encode(this)
