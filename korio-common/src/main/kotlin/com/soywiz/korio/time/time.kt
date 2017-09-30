package com.soywiz.korio.time

expect object STimeProvider {
	fun currentTimeMillis(): Long
}

object TimeProvider {
	fun now(): Long = STimeProvider.currentTimeMillis()
}

expect class UTCDate(time: Long) {
	companion object {
		operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate
	}

	val time: Long
	val fullYear: Int
	val dayOfMonth: Int
	val dayOfWeek: Int
	val month0: Int
	val hours: Int
	val minutes: Int
	val seconds: Int
}

//expect fun UTCDate(time: Long): Date

/*
// @TODO: Does this cause a problem when compiling?
object DateBuilder {
	private val TicksInMillisecond = 10000L
	private val TicksInSecond = TicksInMillisecond * 1000L
	private val DaysToMonth365 = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365)
	private val DaysToMonth366 = intArrayOf(0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366)

	fun getTimestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, milliseconds: Int): Long {
		val timestamp: Long = dateToTicks(year, month, day) + timeToTicks(hour, minute, second)
		return (timestamp + milliseconds * TicksInMillisecond) / TicksInMillisecond
	}

	fun isLeapYear(year: Int): Boolean = (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0)

	private fun dateToTicks(year: Int, month: Int, day: Int): Long {
		if (((year >= 1) && (year <= 9999)) && ((month >= 1) && (month <= 12))) {
			val daysToMonth = if (isLeapYear(year)) DaysToMonth366 else DaysToMonth365
			if ((day >= 1) && (day <= (daysToMonth[month] - daysToMonth[month - 1]))) {
				val previousYear = year - 1
				val daysInPreviousYears = ((((previousYear * 365) + (previousYear / 4)) - (previousYear / 100)) + (previousYear / 400))
				val totalDays = ((daysInPreviousYears + daysToMonth[month - 1]) + day) - 1
				return (totalDays * 0xc92a69c000L)
			}
		}
		invalidArg("out of bounds")
	}

	private fun timeToTicks(hour: Int, minute: Int, second: Int): Long {
		val totalSeconds = ((hour * 3600L) + (minute * 60L)) + second
		if ((totalSeconds > 0xd6bf94d5e5L) || (totalSeconds < -922337203685L)) invalidArg("out of bounds")

		return (totalSeconds * TicksInSecond)
	}
}
*/
