package com.soywiz.korio.crypto

impl class NativeCRC32 {
	val crc32 = java.util.zip.CRC32()

	impl fun update(data: ByteArray, offset: Int, size: Int) {
		crc32.update(data, offset, size)
	}
	impl fun digest(): Int {
		return crc32.value.toInt()
	}
}
