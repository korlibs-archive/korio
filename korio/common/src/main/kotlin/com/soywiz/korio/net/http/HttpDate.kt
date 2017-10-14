package com.soywiz.korio.net.http

import com.soywiz.korio.KorioNative
import com.soywiz.korio.time.DateTime
import com.soywiz.korio.time.SimplerDateFormat

//Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123
//Sunday, 06-Nov-94 08:49:37 GMT ; RFC 850, obsoleted by RFC 1036
//Sun Nov  6 08:49:37 1994       ; ANSI C's asctime() format
object HttpDate {
	val _format = SimplerDateFormat("EEE, dd MMM yyyy HH:mm:ss z")

	fun format(date: Long): String = _format.format(date)
	fun format(date: DateTime): String = _format.format(date.unix)
	fun format(): String = format(KorioNative.currentTimeMillis())

	fun parse(str: String): Long = _format.parse(str)
	fun parseOrNull(str: String?): Long? = try {
		str?.let { _format.parse(str) }
	} catch (e: Throwable) {
		null
	}
}