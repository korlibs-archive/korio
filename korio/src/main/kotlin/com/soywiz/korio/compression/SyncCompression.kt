package com.soywiz.korio.compression

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.io.ByteArrayInputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.InflaterInputStream

actual object SyncCompression {
	actual fun inflate(data: ByteArray): ByteArray {
		val out = ByteOutputStream()
		val s = InflaterInputStream(ByteArrayInputStream(data))
		val temp = ByteArray(0x1000)
		while (true) {
			val read = s.read(temp, 0, temp.size)
			if (read <= 0) break
			out.write(temp, 0, read)
		}
		return out.bytes.copyOf(out.count)
	}

	actual fun inflateTo(data: ByteArray, out: ByteArray): ByteArray {
		val s = InflaterInputStream(ByteArrayInputStream(data))
		var pos = 0
		var remaining = out.size
		while (true) {
			val read = s.read(out, pos, remaining)
			if (read <= 0) break
			pos += read
			remaining -= read
		}
		return out
	}

	actual fun deflate(data: ByteArray, level: Int): ByteArray {
		return DeflaterInputStream(ByteArrayInputStream(data), Deflater(level)).readBytes()
	}
}
