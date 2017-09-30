package com.soywiz.korio.crypto

actual class NativeCRC32 {
	val crc32 = java.util.zip.CRC32()

	actual fun update(data: ByteArray, offset: Int, size: Int) {
		crc32.update(data, offset, size)
	}
	actual fun digest(): Int {
		return crc32.value.toInt()
	}
}
