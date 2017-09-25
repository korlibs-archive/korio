package com.soywiz.korio.time

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.format
import com.soywiz.korio.lang.splitKeep
import com.soywiz.korio.util.substr

class SimplerDateFormat(val format: String) {
	companion object {
		private val rx = Regex("[\\w]+")
		private val englishDaysOfWeek = listOf(
			"sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
		)
		private val englishMonths = listOf(
			"january", "february", "march", "april", "may", "june",
			"july", "august", "september", "october", "november", "december"
		)
		private val englishMonths3 = englishMonths.map { it.substr(0, 3) }
	}

	private val parts = arrayListOf<String>()
	//val escapedFormat = Regex.escape(format)
	private val escapedFormat = Regex.escapeReplacement(format)

	private val rx2: Regex = Regex("^" + escapedFormat.replace(rx) { result ->
		parts += result.groupValues[0]
		"([\\w\\+\\-]+)"
	} + "$")

	private val parts2 = escapedFormat.splitKeep(rx)

	// EEE, dd MMM yyyy HH:mm:ss z -- > Sun, 06 Nov 1994 08:49:37 GMT
	// YYYY-MM-dd HH:mm:ss

	fun format(date: Long): String {
		val dd = UTCDate(date)
		var out = ""
		for (name in parts2) {
			out += when (name) {
				"EEE" -> englishDaysOfWeek[dd.dayOfWeek].substr(0, 3).capitalize()
				"z", "zzz" -> "UTC"
				"d" -> "%d".format(dd.dayOfMonth)
				"dd" -> "%02d".format(dd.dayOfMonth)
				"MM" -> "%02d".format(dd.month0 + 1)
				"MMM" -> englishMonths[dd.month0].substr(0, 3).capitalize()
				"yyyy" -> "%04d".format(dd.fullYear)
				"YYYY" -> "%04d".format(dd.fullYear)
				"HH" -> "%02d".format(dd.hours)
				"mm" -> "%02d".format(dd.minutes)
				"ss" -> "%02d".format(dd.seconds)
				else -> name
			}
		}
		return out
	}

	fun parse(str: String): Long {
		var second = 0
		var minute = 0
		var hour = 0
		var day = 1
		var month0 = 0
		var fullYear = 1970
		val result = rx2.find(str) ?: invalidOp("Not a valid format: '$str' for '$format'")
		for ((name, value) in parts.zip(result.groupValues.drop(1))) {
			when (name) {
				"EEE" -> Unit // day of week (Sun)
				"z", "zzz" -> Unit // timezone (GMT)
				"d", "dd" -> day = value.toInt()
				"MM" -> month0 = value.toInt() - 1
				"MMM" -> month0 = englishMonths3.indexOf(value.toLowerCase())
				"yyyy", "YYYY" -> fullYear = value.toInt()
				"HH" -> hour = value.toInt()
				"mm" -> minute = value.toInt()
				"ss" -> second = value.toInt()
				else -> {
					// ...
				}
			}
		}
		//println("year=$year, month=$month, day=$day, hour=$hour, minute=$minute, second=$second")
		return UTCDate(fullYear, month0, day, hour, minute, second).time
	}
}