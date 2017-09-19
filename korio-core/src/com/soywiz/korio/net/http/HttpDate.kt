package com.soywiz.korio.net.http

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object HttpDate {
	val GMT = TimeZone.getTimeZone("GMT")
	val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
		timeZone = GMT
	}

	fun format(date: Long): String = format.format(Date(date))
	fun format(date: Date): String = format.format(date)
	fun format(): String = format(Calendar.getInstance().time)
	fun parse(str: String): Date = format.parse(str)
	fun parseOrNull(str: String?): Date? = try { str?.let { format.parse(str) } } catch (e: ParseException) { null }
}