package com.soywiz.korio.compression

import com.soywiz.korio.async.executeInWorker

object Compression {
	suspend fun uncompressGzip(data: ByteArray): ByteArray = executeInWorker {
		//val out = ByteArrayOutputStream()
		//GZIPInputStream(ByteArrayInputStream(data)).copyTo(out)
		//return@executeInWorker out.toByteArray()
		TODO()
	}

	suspend fun uncompressZlib(data: ByteArray): ByteArray = executeInWorker {
		TODO()
		//val out = ByteArrayOutputStream()
		//InflaterInputStream(ByteArrayInputStream(data)).copyTo(out)
		//return@executeInWorker out.toByteArray()
	}

	suspend fun compressGzip(data: ByteArray): ByteArray = executeInWorker {
		TODO()
		//val out = ByteArrayOutputStream()
		//val out2 = GZIPOutputStream(out)
		//ByteArrayInputStream(data).copyTo(out2)
		//out2.flush()
		//return@executeInWorker out.toByteArray()
	}

	suspend fun compressZlib(data: ByteArray, level: Int = 6): ByteArray = executeInWorker {
		TODO()
		//val out = ByteArrayOutputStream()
		//val deflater = Deflater(level)
		//val out2 = DeflaterOutputStream(out, deflater)
		//ByteArrayInputStream(data).copyTo(out2)
		//out2.flush()
		//return@executeInWorker out.toByteArray()
	}
}

suspend fun ByteArray.uncompressGzip() = Compression.uncompressGzip(this)
suspend fun ByteArray.uncompressZlib() = Compression.uncompressZlib(this)
suspend fun ByteArray.compressGzip() = Compression.compressGzip(this)
suspend fun ByteArray.compressZlib() = Compression.compressZlib(this)
