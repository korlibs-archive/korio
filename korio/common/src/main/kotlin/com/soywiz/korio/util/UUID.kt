package com.soywiz.korio.util

import com.soywiz.korio.KorioNative
import com.soywiz.korio.crypto.Hex
import com.soywiz.korio.error.invalidArg
import com.soywiz.korio.lang.format

class UUID(val data: ByteArray) {
	companion object {
		private val regex = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", RegexOption.IGNORE_CASE)

		private fun fix(data: ByteArray, version: Int, variant: Int): ByteArray {
			data[6] = (data[6].toInt() and 0b1111_0000) or (version shl 4)
			data[8] = (data[8].toInt() and 0x11_000000) or (variant shl 6)
			return data
		}

		fun randomUUID(): UUID = UUID(fix(ByteArray(16).apply {
			KorioNative.getRandomValues(this)
		}, version = 4, variant = 1))

		operator fun invoke(str: String): UUID {
			if (regex.matchEntire(str) == null) invalidArg("Invalid UUID")
			return UUID(Hex.decode(str.replace("-", "")))
		}
	}

	val version: Int get() = (data[6].toInt() ushr 4) and 0b1111
	val variant: Int get() = (data[8].toInt() ushr 6) and 0b11

	override fun toString() = "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x".format(
		data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7],
		data[8], data[9], data[10], data[11], data[12], data[13], data[14], data[15]
	)
}