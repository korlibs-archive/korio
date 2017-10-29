package com.soywiz.korio.time

import com.soywiz.klock.SimplerDateFormat
import kotlin.test.assertEquals

class SimplerDateFormatTest {
	// Sun, 06 Nov 1994 08:49:37 GMT
	val format = SimplerDateFormat("EEE, dd MMM yyyy HH:mm:ss z")

	@kotlin.test.Test
	fun testParse() {
		assertEquals(784111777000, format.parse("Sun, 06 Nov 1994 08:49:37 UTC"))
	}

	@kotlin.test.Test
	fun testFormat() {
		assertEquals("Sun, 06 Nov 1994 08:49:37 UTC", format.format(784111777000))
	}

	@kotlin.test.Test
	fun testParseFormat() {
		val dateStr = "Sun, 06 Nov 1994 08:49:37 UTC"
		assertEquals(dateStr, format.format(format.parse(dateStr)))
	}
}