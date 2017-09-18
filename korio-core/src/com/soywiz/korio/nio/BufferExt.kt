package com.soywiz.korio.nio

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset

fun ByteBuffer.toByteArray(): ByteArray {
	val byteBuffer = this
	val data = ByteArray(byteBuffer.capacity())
	(byteBuffer.duplicate().clear() as ByteBuffer).get(data)
	return data
}
fun ByteBuffer.toCharBuffer(charset: Charset = Charsets.UTF_8): CharBuffer = charset.decode(this)
fun ByteBuffer.toString(charset: Charset = Charsets.UTF_8): String = this.toCharBuffer(charset).toString()
fun CharBuffer.toByteBuffer(charset: Charset = Charsets.UTF_8): ByteBuffer = charset.encode(this)
fun CharBuffer.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = this.toByteBuffer(charset).toByteArray()
