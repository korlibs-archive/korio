package com.soywiz.korio.util.checksum

object CRC32 : SimpleChecksum {
	override val initialValue = 0

	private val CRC_TABLE = IntArray(0x100).apply {
		val poly = 0xEDB88320.toInt()
		for (n in 0 until 0x100) {
			var c = n
			for (k in 0 until 8) c = (if ((c and 1) != 0) poly xor (c ushr 1) else c ushr 1)
			this[n] = c
		}
	}

	override fun update(old: Int, data: ByteArray, offset: Int, len: Int): Int {
		var c = old.inv()
		val table = CRC_TABLE
		for (n in offset until offset + len) c = table[(c xor (data[n].toInt() and 0xFF)) and 0xff] xor (c ushr 8)
		return c.inv()
	}
}
