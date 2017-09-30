package com.soywiz.korio.compression

actual object Compression {
	suspend actual fun uncompressGzip(data: ByteArray): ByteArray = TODO()
	suspend actual fun uncompressZlib(data: ByteArray): ByteArray = TODO()
	suspend actual fun compressGzip(data: ByteArray, level: Int): ByteArray = TODO()
	suspend actual fun compressZlib(data: ByteArray, level: Int): ByteArray = TODO()
}
