package com.soywiz.korio.compression

expect object SyncCompression {
	fun inflate(data: ByteArray): ByteArray
	fun inflateTo(data: ByteArray, out: ByteArray): ByteArray
	fun deflate(data: ByteArray, level: Int): ByteArray
}

//fun SyncCompression.inflateTo(input: ByteArray, output: ByteArray): Int {
//	val uncompressed = SyncCompression.inflate(input)
//	val size = min(uncompressed.size, output.size)
//	uncompressed.copyRangeTo(0, output, 0, size)
//	return size
//}
