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
	constructor(year: Int, month: Int, day: Int, hours: Int, minutes: Int, seconds: Int) : this(-1) {
		TODO()
	}

	companion object {
		fun UTC(year: Int, month: Int, day: Int, hours: Int, minutes: Int, seconds: Int): Long = TODO()
	}
}