package com.soywiz.korio.time

import java.util.*

//impl fun UTCDate(time: Long): Date {
//	//val millis = ZoneOffset.systemDefault().rules.getOffset(LocalDateTime.now()).totalSeconds * 1000L
//	val millis = ZoneOffset.systemDefault().rules.getOffset(Instant.now()).totalSeconds * 1000L
//	return java.util.Date(time - millis)
//}

@Suppress("DEPRECATION", "CanBeParameter")
// @TODO: Kotlin BUG: Constructor parameter is never used as a property
impl class UTCDate impl constructor(impl val time: Long) {
	companion impl object {
		private val UTC_OFFSET = Date(2000, 1, 1, 0, 0, 0).time - Date.UTC(2000, 1, 1, 0, 0, 0)
		impl operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate {
			return UTCDate(java.util.Date.UTC(fullYear - 1900, month0, day, hours, minutes, seconds))
		}
	}

	val jdate = java.util.Date(time + UTC_OFFSET)

	impl val fullYear: Int get() = jdate.year + 1900
	impl val dayOfMonth: Int get() = jdate.date
	impl val dayOfWeek: Int get() = jdate.day
	impl val month0: Int get() = jdate.month
	impl val hours: Int get() = jdate.hours
	impl val minutes: Int get() = jdate.minutes
	impl val seconds: Int get() = jdate.seconds
}


@Suppress("DEPRECATION")
impl fun dateToTimestamp(year: Int, month: Int, day: Int, hours: Int, minutes: Int, seconds: Int): Long {
	return java.util.Date.UTC(year, month, day, hours, minutes, seconds)
}

impl fun currentTimeMillis(): Long = System.currentTimeMillis()
