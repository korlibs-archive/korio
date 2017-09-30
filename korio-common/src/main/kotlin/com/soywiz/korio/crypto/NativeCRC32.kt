package com.soywiz.korio.crypto

expect class NativeCRC32 {
	fun update(data: ByteArray, offset: Int, size: Int)
	fun digest(): Int
}
