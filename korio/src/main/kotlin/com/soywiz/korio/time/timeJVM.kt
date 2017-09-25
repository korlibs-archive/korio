package com.soywiz.korio.time

import java.util.*
import jdk.nashorn.internal.objects.NativeDate.getTime
import java.util.GregorianCalendar



//impl fun UTCDate(time: Long): Date {
//	//val millis = ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds * 1000L
//	val millis = ZoneOffset.systemDefault().rules.getOffset(Instant.now()).totalSeconds * 1000L
//	return java.util.Date(time - millis)
//}

@Suppress("DEPRECATION", "CanBeParameter")
// @TODO: Kotlin BUG: Constructor parameter is never used as a property
impl class UTCDate impl constructor(impl val time: Long) {
	companion impl object {
		private val UTC = TimeZone.getTimeZone("UTC")

		impl operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate {
			return UTCDate(GregorianCalendar(fullYear, month0, day, hours, minutes, seconds).apply {
				timeZone = UTC
			}.timeInMillis)
		}
	}

	val jdate = GregorianCalendar.getInstance(UTC).apply {
		timeInMillis = this@UTCDate.time
	}

	impl val fullYear: Int get() = jdate.get(Calendar.YEAR)
	impl val dayOfMonth: Int get() = jdate.get(Calendar.DAY_OF_MONTH)
	impl val dayOfWeek: Int get() = jdate.get(Calendar.DAY_OF_WEEK) - 1
	impl val month0: Int get() = jdate.get(Calendar.MONTH)
	impl val hours: Int get() = jdate.get(Calendar.HOUR)
	impl val minutes: Int get() = jdate.get(Calendar.MINUTE)
	impl val seconds: Int get() = jdate.get(Calendar.SECOND)
}

impl object STimeProvider {
	impl fun currentTimeMillis(): Long = System.currentTimeMillis()
}
