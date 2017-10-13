package com.soywiz.korio.time

import com.soywiz.korio.KorioNative
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.format
import com.soywiz.korio.lang.splitKeep
import com.soywiz.korio.math.clamp
import com.soywiz.korio.util.substr

enum class DayOfWeek(val index: Int) {
	Sunday(0), Monday(1), Tuesday(2), Wednesday(3), Thursday(4), Friday(5), Saturday(6);

	companion object {
		val BY_INDEX = values()
		operator fun get(index: Int) = BY_INDEX[index]
	}
}

class DateException(msg: String) : RuntimeException(msg)

// From .NET DateTime
class DateTime private constructor(dummy: Boolean, val internalTicks: Long) {
	companion object {
		operator fun invoke(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0, milliseconds: Int = 0): DateTime {
			return DateTime(true, dateToTicks(year, month, day) + timeToTicks(hour, minute, second) + milliseconds * TICKS_PER_MILLISECOND)
		}

		operator fun invoke(time: Long) = fromUnix(time)

		fun createAdjusted(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0, milliseconds: Int = 0): DateTime {
			val dy = clamp(year, 1, 9999)
			val dm = clamp(month, 1, 12)
			val dd = clamp(day, 1, daysInMonth(dy, dm))
			val th = clamp(hour, 0, 23)
			val tm = clamp(minute, 0, 59)
			val ts = clamp(second, 0, 59)
			return DateTime(dy, dm, dd, th, tm, ts, milliseconds)
		}

		fun fromUnix(time: Long): DateTime = DateTime(true, EPOCH_TICKS + time * TICKS_PER_MILLISECOND)

		fun nowUnix() = KorioNative.currentTimeMillis()
		fun now() = fromUnix(nowUnix())

		val EPOCH by lazy { DateTime(1970, 1, 1, 0, 0, 0) }
		val EPOCH_TICKS by lazy { EPOCH.internalTicks }

		private const val TICKS_PER_MILLISECOND: Long = 1
		private const val TICKS_PER_SECOND: Long = TICKS_PER_MILLISECOND * 1000
		private const val TICKS_PER_MINUTE: Long = TICKS_PER_SECOND * 60
		private const val TICKS_PER_HOUR: Long = TICKS_PER_MINUTE * 60
		private const val TICKS_PER_DAY: Long = TICKS_PER_HOUR * 24

		private const val MILLIS_PER_SECOND = 1000
		private const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60
		private const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60
		private const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24

		private const val DAYS_PER_YEAR = 365
		private const val DAYS_PER_4_YEARS = DAYS_PER_YEAR * 4 + 1
		private const val DAYS_PER_100_YEARS = DAYS_PER_4_YEARS * 25 - 1
		private const val DAYS_PER_400_YEARS = DAYS_PER_100_YEARS * 4 + 1

		private const val DATE_PART_YEAR = 0
		private const val DATE_PART_DAY_OF_YEAR = 1
		private const val DATE_PART_MONTH = 2
		private const val DATE_PART_DAY = 3

		private val DaysToMonth366 = intArrayOf(0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366)
		private val DaysToMonth365 = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365)

		private fun checkYear(year: Int) {
			if (year !in 1..9999) throw DateException("Year $year not in 1..9999")
		}

		private fun checkMonth(month: Int) {
			if (month !in 1..12) throw DateException("Month $month not in 1..12")
		}

		fun isLeapYear(year: Int): Boolean {
			checkYear(year)
			return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
		}

		private fun dateToTicks(year: Int, month: Int, day: Int): Long {
			checkYear(year)
			checkMonth(month)
			val days = if (isLeapYear(year)) DaysToMonth366 else DaysToMonth365
			if (day !in 1..(days[month] - days[month - 1])) throw DateException("Day $day not valid for year=$year and month=$month")
			val y = year - 1
			val n = y * 365 + y / 4 - y / 100 + y / 400 + days[month - 1] + day - 1
			return n * TICKS_PER_DAY
		}

		private fun timeToTicks(hour: Int, minute: Int, second: Int): Long {
			if (hour !in 0..23) throw DateException("Hour $hour not in 0..23")
			if (minute !in 0..59) throw DateException("Minute $minute not in 0..59")
			if (second !in 0..59) throw DateException("Second $second not in 0..59")
			val totalSeconds = hour.toLong() * 3600 + minute.toLong() * 60 + second.toLong()
			return totalSeconds * TICKS_PER_SECOND
		}

		fun daysInMonth(month: Int, isLeap: Boolean): Int {
			checkMonth(month)
			val days = if (isLeap) DaysToMonth366 else DaysToMonth365
			return days[month] - days[month - 1]
		}

		fun daysInMonth(year: Int, month: Int): Int = daysInMonth(month, isLeapYear(year))

		private fun getDatePart(InternalTicks: Long, part: Int): Int {
			val ticks = InternalTicks
			var n = (ticks / TICKS_PER_DAY).toInt()
			val y400 = n / DAYS_PER_400_YEARS
			n -= y400 * DAYS_PER_400_YEARS
			var y100 = n / DAYS_PER_100_YEARS
			if (y100 == 4) y100 = 3
			n -= y100 * DAYS_PER_100_YEARS
			val y4 = n / DAYS_PER_4_YEARS
			n -= y4 * DAYS_PER_4_YEARS
			var y1 = n / DAYS_PER_YEAR
			if (y1 == 4) y1 = 3
			if (part == DATE_PART_YEAR) return y400 * 400 + y100 * 100 + y4 * 4 + y1 + 1
			n -= y1 * DAYS_PER_YEAR
			if (part == DATE_PART_DAY_OF_YEAR) return n + 1
			val leapYear = y1 == 3 && (y4 != 24 || y100 == 3)
			val days = if (leapYear) DaysToMonth366 else DaysToMonth365
			var m = n shr 5 + 1
			while (n >= days[m]) m++
			return if (part == DATE_PART_MONTH) m else n - days[m - 1] + 1
		}
	}

	private fun getDatePart(part: Int): Int = Companion.getDatePart(internalTicks, part)

	val unix: Long get() = (internalTicks - EPOCH.internalTicks) / TICKS_PER_MILLISECOND
	val time: Long get() = unix
	val year: Int get() = fullYear
	val fullYear: Int get() = getDatePart(DATE_PART_YEAR)
	val month: Int get() = getDatePart(DATE_PART_MONTH)
	val month0: Int get() = month - 1
	val month1: Int get() = month
	val dayOfMonth: Int get() = getDatePart(DATE_PART_DAY)
	val dayOfWeek: Int get() = ((internalTicks / TICKS_PER_DAY + 1) % 7).toInt()
	val dayOfWeekEnum: DayOfWeek get() = DayOfWeek[dayOfWeek]
	val dayOfYear: Int get() = getDatePart(DATE_PART_DAY_OF_YEAR)
	val hours: Int get() = (((internalTicks / TICKS_PER_HOUR) % 24).toInt())
	val minutes: Int get() = ((internalTicks / TICKS_PER_MINUTE) % 60).toInt()
	val seconds: Int get() = ((internalTicks / TICKS_PER_SECOND) % 60).toInt()
	val milliseconds: Int get() = ((internalTicks / TICKS_PER_MILLISECOND) % 1000).toInt()

	fun addYears(value: Int): DateTime = addMonths(value * 12)

	fun addMonths(months: Int): DateTime {
		var y = getDatePart(DATE_PART_YEAR)
		var m = getDatePart(DATE_PART_MONTH)
		var d = getDatePart(DATE_PART_DAY)
		val i = m - 1 + months
		if (i >= 0) {
			m = i % 12 + 1
			y += i / 12
		} else {
			m = 12 + (i + 1) % 12
			y += (i - 11) / 12
		}
		checkYear(y)
		val days = daysInMonth(y, m)
		if (d > days) d = days
		return DateTime(true, (dateToTicks(y, m, d) + internalTicks % TICKS_PER_DAY))
	}

	fun addDays(value: Double): DateTime = add(value, MILLIS_PER_DAY)
	fun addHours(value: Double): DateTime = add(value, MILLIS_PER_HOUR)
	fun addMinutes(value: Double): DateTime = add(value, MILLIS_PER_MINUTE)
	fun addSeconds(value: Double): DateTime = add(value, MILLIS_PER_SECOND)
	fun addMilliseconds(value: Double): DateTime = add(value, 1)

	fun addTicks(value: Long): DateTime {
		val ticks = internalTicks
		return DateTime(true, (ticks + value))
	}

	private fun add(value: Double, scale: Int): DateTime {
		if (value == 0.0) return this
		val millis = (value * scale + if (value >= 0) 0.5 else -0.5).toLong()
		return addTicks(millis * TICKS_PER_MILLISECOND)
	}

	operator fun plus(add: TimeDistance) = this
		.addMonths(add.years * 12 + add.months)
		.addMilliseconds(add.days * MILLIS_PER_DAY + add.hours * MILLIS_PER_HOUR + add.minutes * MILLIS_PER_MINUTE + add.seconds * MILLIS_PER_SECOND + add.milliseconds)

	operator fun compareTo(other: DateTime): Int = this.internalTicks.compareTo(other.internalTicks)
	override fun hashCode(): Int = internalTicks.hashCode()
	override fun equals(other: Any?): Boolean = this.internalTicks == (other as? DateTime?)?.internalTicks
	override fun toString(): String = SimplerDateFormat.DEFAULT_FORMAT.format(this)
	fun toString(format: String): String = SimplerDateFormat(format).format(this)
}

data class TimeDistance(val years: Int = 0, val months: Int = 0, val days: Double = 0.0, val hours: Double = 0.0, val minutes: Double = 0.0, val seconds: Double = 0.0, val milliseconds: Double = 0.0) {
	operator fun plus(other: TimeDistance) = TimeDistance(
		years + other.years,
		months + other.months,
		days + other.days,
		hours + other.hours,
		minutes + other.minutes,
		seconds + other.seconds,
		milliseconds + other.milliseconds
	)
}

inline val Int.years get() = TimeDistance(years = this)
inline val Int.months get() = TimeDistance(months = this)
inline val Number.days get() = TimeDistance(days = this.toDouble())
inline val Number.hours get() = TimeDistance(hours = this.toDouble())
inline val Number.minutes get() = TimeDistance(minutes = this.toDouble())
//inline val Number.seconds get() = TimeAdd(seconds = this.toDouble())

@Suppress("DataClassPrivateConstructor")
//data class TimeSpan private constructor(val ms: Int) : Comparable<TimeSpan>, Interpolable<TimeSpan> {
data class TimeSpan private constructor(val ms: Int) : Comparable<TimeSpan> {
	val milliseconds: Int get() = this.ms
	val seconds: Double get() = this.ms.toDouble() / 1000.0

	companion object {
		val ZERO = TimeSpan(0)
		@PublishedApi internal fun fromMilliseconds(ms: Int) = when (ms) {
			0 -> ZERO
			else -> TimeSpan(ms)
		}
	}

	override fun compareTo(other: TimeSpan): Int = this.ms.compareTo(other.ms)

	operator fun plus(other: TimeSpan): TimeSpan = TimeSpan(this.ms + other.ms)
	operator fun minus(other: TimeSpan): TimeSpan = TimeSpan(this.ms - other.ms)
	operator fun times(scale: Int): TimeSpan = TimeSpan(this.ms * scale)
	operator fun times(scale: Double): TimeSpan = TimeSpan((this.ms * scale).toInt())

	//override fun interpolateWith(other: TimeSpan, ratio: Double): TimeSpan = TimeSpan(ratio.interpolate(this.ms, other.ms))
}

inline val Number.milliseconds get() = TimeSpan.fromMilliseconds(this.toInt())
inline val Number.seconds get() = TimeSpan.fromMilliseconds((this.toDouble() * 1000.0).toInt())

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

		val DEFAULT_FORMAT by lazy { SimplerDateFormat("EEE, dd MMM yyyy HH:mm:ss z") }
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

	fun format(date: Long): String = format(DateTime.fromUnix(date))

	fun format(dd: DateTime): String {
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
		return DateTime(fullYear, month0 + 1, day, hour, minute, second).time
	}
}
