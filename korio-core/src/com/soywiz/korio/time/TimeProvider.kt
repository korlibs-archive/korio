package com.soywiz.korio.time

header object STimeProvider {
	fun now(): Long
}

/*
object STimeProvider {
	fun now(): Long = TODO()
}
*/

object TimeProvider {
	fun now(): Long = STimeProvider.now()
}

class Date(val time: Long) {
}