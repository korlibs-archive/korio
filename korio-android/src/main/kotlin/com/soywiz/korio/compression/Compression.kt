package com.soywiz.korio.compression

import com.soywiz.korio.async.executeInWorker
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.*

actual object Compression {
	actual suspend fun uncompressGzip(data: ByteArray): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		GZIPInputStream(ByteArrayInputStream(data)).copyTo(out)
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun uncompressZlib(data: ByteArray): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		InflaterInputStream(ByteArrayInputStream(data)).copyTo(out)
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun compressGzip(data: ByteArray, level: Int): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		val out2 = GZIPOutputStream(out)
		ByteArrayInputStream(data).copyTo(out2)
		out2.flush()
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun compressZlib(data: ByteArray, level: Int): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		val deflater = Deflater(level)
		val out2 = DeflaterOutputStream(out, deflater)
		ByteArrayInputStream(data).copyTo(out2)
		out2.flush()
		return@executeInWorker out.toByteArray()
	}
}