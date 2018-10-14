package com.soywiz.korio.crypto

interface SimpleChecksum {
	val INITIAL: Int
	fun update(old: Int, data: ByteArray, offset: Int = 0, len: Int = data.size - offset): Int

	companion object {
		val DUMMY = object : SimpleChecksum {
			override val INITIAL: Int = 0
			override fun update(old: Int, data: ByteArray, offset: Int, len: Int): Int = 0
		}
	}
}

fun SimpleChecksum.compute(data: ByteArray, offset: Int = 0, len: Int = data.size - offset) =
	update(INITIAL, data, offset, len)

object Adler32 : SimpleChecksum {
	private val adler_base = 65521

	override val INITIAL = 1

	override fun update(old: Int, data: ByteArray, offset: Int, len: Int): Int {
		var s1 = (old ushr 0) and 0xffff
		var s2 = (old ushr 16) and 0xffff

		for (n in offset until offset + len) {
			s1 = (s1 + (data[n].toInt() and 0xFF)) % adler_base
			s2 = (s2 + s1) % adler_base
		}
		return (s2 shl 16) or s1
	}
}

object CRC32 : SimpleChecksum {
	override val INITIAL = 0

	private val crc_table = IntArray(0x100).apply {
		val POLY = 0xEDB88320.toInt()
		for (n in 0 until 0x100) {
			var c = n
			for (k in 0 until 8) c = (if ((c and 1) != 0) POLY xor (c ushr 1) else c ushr 1)
			this[n] = c
		}

	}

	override fun update(old: Int, data: ByteArray, offset: Int, len: Int): Int {
		var c = old.inv()
		val table = this.crc_table
		for (n in offset until offset + len) c = table[(c xor (data[n].toInt() and 0xFF)) and 0xff] xor (c ushr 8)
		return c.inv()
	}
}