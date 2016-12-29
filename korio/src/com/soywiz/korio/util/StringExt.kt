package com.soywiz.korio.util

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

fun String.toBytez(len: Int, charset: Charset = Charsets.UTF_8): ByteArray {
	val out = ByteArrayOutputStream()
	out.write(this.toByteArray(charset))
	while (out.size() < len) out.write(0)
	return out.toByteArray()
}

fun String.toBytez(charset: Charset = Charsets.UTF_8): ByteArray {
	val out = ByteArrayOutputStream()
	out.write(this.toByteArray(charset))
	out.write(0)
	return out.toByteArray()
}