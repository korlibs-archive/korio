package com.soywiz.korio.time

import org.w3c.dom.get
import kotlin.browser.window
import kotlin.js.Date

impl object STimeProvider {
	impl fun currentTimeMillis() = Date().getTime().toLong()
}

impl class UTCDate impl constructor(time: Long) {
	val date = Date().asDynamic()

	companion impl object {
		impl operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate {
			return UTCDate(window["Date"].asDynamic().UTC(fullYear, month0, day, hours, minutes, seconds))
		}
	}

	impl val time: Long get() = (date.getTime() as Double).toLong()
	impl val fullYear: Int get() = date.getUTCFullYear()
	impl val dayOfMonth: Int get() = date.getUTCDate()
	impl val dayOfWeek: Int get() = date.getUTCDay()
	impl val month0: Int get() = date.getUTCMonth()
	impl val hours: Int get() = date.getUTCHours()
	impl val minutes: Int get() = date.getUTCMinutes()
	impl val seconds: Int get() = date.getUTCSeconds()
}
