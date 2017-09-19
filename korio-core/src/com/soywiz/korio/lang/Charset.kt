package com.soywiz.korio.lang

class Charset {

}

object Charsets {
	val UTF_8 = Charset()
}

fun String.toByteArray(charset: Charset): ByteArray = TODO()
fun ByteArray.toString(charset: Charset): String = TODO()