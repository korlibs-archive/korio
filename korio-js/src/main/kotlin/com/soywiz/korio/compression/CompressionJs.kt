package com.soywiz.korio.compression

impl object Compression {
	suspend impl fun uncompressGzip(data: ByteArray): ByteArray = TODO()
	suspend impl fun uncompressZlib(data: ByteArray): ByteArray = TODO()
	suspend impl fun compressGzip(data: ByteArray, level: Int): ByteArray = TODO()
	suspend impl fun compressZlib(data: ByteArray, level: Int): ByteArray = TODO()
}
