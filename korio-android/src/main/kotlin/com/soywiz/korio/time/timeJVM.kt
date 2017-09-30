package com.soywiz.korio.time

import java.util.*
import jdk.nashorn.internal.objects.NativeDate.getTime
import java.util.GregorianCalendar



//actual fun UTCDate(time: Long): Date {
//	//val millis = ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds * 1000L
//	val millis = ZoneOffset.systemDefault().rules.getOffset(Instant.now()).totalSeconds * 1000L
//	return java.util.Date(time - millis)
//}

@Suppress("DEPRECATION", "CanBeParameter")
// @TODO: Kotlin BUG: Constructor parameter is never used as a property
actual class UTCDate actual constructor(actual val time: Long) {
	companion actual object {
		private val UTC = TimeZone.getTimeZone("UTC")

		actual operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate {
			return UTCDate(GregorianCalendar(fullYear, month0, day, hours, minutes, seconds).apply {
				timeZone = UTC
			}.timeInMillis)
		}
	}

	val jdate = GregorianCalendar.getInstance(UTC).apply {
		timeInMillis = this@UTCDate.time
	}

	actual val fullYear: Int get() = jdate.get(Calendar.YEAR)
	actual val dayOfMonth: Int get() = jdate.get(Calendar.DAY_OF_MONTH)
	actual val dayOfWeek: Int get() = jdate.get(Calendar.DAY_OF_WEEK) - 1
	actual val month0: Int get() = jdate.get(Calendar.MONTH)
	actual val hours: Int get() = jdate.get(Calendar.HOUR)
	actual val minutes: Int get() = jdate.get(Calendar.MINUTE)
	actual val seconds: Int get() = jdate.get(Calendar.SECOND)
}

actual object STimeProvider {
	actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
