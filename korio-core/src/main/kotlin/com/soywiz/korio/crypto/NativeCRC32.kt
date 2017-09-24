package com.soywiz.korio.crypto

header class NativeCRC32 {
	fun update(data: ByteArray, offset: Int, size: Int)
	fun digest(): Int
}
