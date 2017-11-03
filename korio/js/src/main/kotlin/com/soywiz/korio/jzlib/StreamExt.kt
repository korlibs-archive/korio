package com.soywiz.korio.jzlib

fun InputStream.copyTo(out: OutputStream) {
	val temp = ByteArray(1024)
	while (true) {
		val read = this.read(temp)
		if (read <= 0) break
		out.write(temp, 0, read)
	}
}