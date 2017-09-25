package com.soywiz.korio.compression

header object Compression {
	suspend fun uncompressGzip(data: ByteArray): ByteArray
	suspend fun uncompressZlib(data: ByteArray): ByteArray
	suspend fun compressGzip(data: ByteArray, level: Int): ByteArray
	suspend fun compressZlib(data: ByteArray, level: Int): ByteArray
}

suspend fun ByteArray.uncompressGzip() = Compression.uncompressGzip(this)
suspend fun ByteArray.uncompressZlib() = Compression.uncompressZlib(this)
suspend fun ByteArray.compressGzip(level: Int = 6) = Compression.compressGzip(this, level)
suspend fun ByteArray.compressZlib(level: Int = 6) = Compression.compressZlib(this, level)
