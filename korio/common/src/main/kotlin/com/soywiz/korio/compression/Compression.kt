package com.soywiz.korio.compression

import com.soywiz.korio.KorioNative

object Compression {
	suspend fun uncompressGzip(data: ByteArray): ByteArray = KorioNative.uncompressGzip(data)
	suspend fun uncompressZlib(data: ByteArray): ByteArray = KorioNative.uncompressZlib(data)
	suspend fun uncompressZlibRaw(data: ByteArray): ByteArray = KorioNative.uncompressZlibRaw(data)
	suspend fun compressGzip(data: ByteArray, level: Int): ByteArray = KorioNative.uncompressGzip(data)
	suspend fun compressZlib(data: ByteArray, level: Int): ByteArray = KorioNative.uncompressZlib(data)
	suspend fun compressZlibRaw(data: ByteArray, level: Int): ByteArray = KorioNative.uncompressZlibRaw(data)
}

suspend fun ByteArray.uncompressGzip() = KorioNative.uncompressGzip(this)
suspend fun ByteArray.uncompressZlib() = KorioNative.uncompressZlib(this)
suspend fun ByteArray.uncompressZlibRaw() = KorioNative.uncompressZlibRaw(this)
suspend fun ByteArray.compressGzip(level: Int = 6) = KorioNative.compressGzip(this, level)
suspend fun ByteArray.compressZlib(level: Int = 6) = KorioNative.compressZlib(this, level)
suspend fun ByteArray.compressZlibRaw(level: Int = 6) = KorioNative.compressZlibRaw(this, level)
