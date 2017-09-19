package com.soywiz.korio.lang

data class Charset(val name: String)

object Charsets {
	val UTF_8 = Charset("UTF-8")
}

fun String.toByteArray(charset: Charset): ByteArray = TODO()
fun ByteArray.toString(charset: Charset): String = TODO()