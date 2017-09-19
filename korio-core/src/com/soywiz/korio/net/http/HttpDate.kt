package com.soywiz.korio.net.http

import java.text.SimpleDateFormat
import java.util.*

object HttpDate {
	val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("GMT")
	}

	fun format(date: Date): String = format.format(date)
	fun format(): String = format(Calendar.getInstance().time)
	fun parse(str: String): Date = format.parse(str)
}