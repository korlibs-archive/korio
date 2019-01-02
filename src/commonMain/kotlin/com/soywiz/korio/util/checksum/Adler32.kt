package com.soywiz.korio.util.checksum

object Adler32 : SimpleChecksum {
	private val adler_base = 65521

	override val initialValue = 1

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
