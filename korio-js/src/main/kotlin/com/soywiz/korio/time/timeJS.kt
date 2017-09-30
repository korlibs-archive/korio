package com.soywiz.korio.time

import org.w3c.dom.get
import kotlin.browser.window
import kotlin.js.Date

actual object STimeProvider {
	actual fun currentTimeMillis() = Date().getTime().toLong()
}

actual class UTCDate actual constructor(time: Long) {
	val date = Date().asDynamic()

	companion actual object {
		actual operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate {
			return UTCDate(window["Date"].asDynamic().UTC(fullYear, month0, day, hours, minutes, seconds))
		}
	}

	actual val time: Long get() = (date.getTime() as Double).toLong()
	actual val fullYear: Int get() = date.getUTCFullYear()
	actual val dayOfMonth: Int get() = date.getUTCDate()
	actual val dayOfWeek: Int get() = date.getUTCDay()
	actual val month0: Int get() = date.getUTCMonth()
	actual val hours: Int get() = date.getUTCHours()
	actual val minutes: Int get() = date.getUTCMinutes()
	actual val seconds: Int get() = date.getUTCSeconds()
}
