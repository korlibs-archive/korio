package com.soywiz.korio.time

class Locale {
	companion object {
		val ENGLISH = Locale()
	}
}

class TimeZone {
	companion object {
		fun getTimeZone(name: String) = TimeZone()
		val UTC = getTimeZone("UTC")
	}
}

class SimpleDateFormat(val format: String, val locale: Locale = Locale.ENGLISH) {
	var timeZone: TimeZone = TimeZone.UTC

	fun format(date: Date): String = TODO()
	fun parse(str: String): Date = TODO()
}