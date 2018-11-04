package com.soywiz.korio.time

import com.soywiz.klock.*
import kotlin.test.*

class TimeTest {
	@kotlin.test.Test
	fun name() {
		assertEquals("Thu, 01 Jan 1970 00:00:00 UTC", DateTime.EPOCH.toString())
		assertEquals("Fri, 13 Oct 2017 22:58:29 UTC", DateTime.fromUnix(1507935509943).toString())
		assertEquals("Sat, 13 Oct 2018 22:58:29 UTC", (DateTime.fromUnix(1507935509943) + 1.years).toString())
		assertEquals(
			"Tue, 13 Nov 2018 22:58:29 UTC",
			(DateTime.fromUnix(1507935509943) + (1.years + 1.months)).toString()
		)
		assertEquals("Thu, 12 Oct 2017 22:58:29 UTC", (DateTime.fromUnix(1507935509943) - 1.days).toString())

		assertEquals("Sat, 14 Oct 2017 00:58:29 GMT+0200", DateTime.fromUnix(1507935509943).toOffset(120.minutes).toString())

		//assertEquals("------", DateTime.nowLocal().toString())

		//new Date(1507935509943)
		//Sat Oct 14 2017 00:58:29 GMT+0200 (Romance Summer Time)
		//new Date(1507935509943 - 1000 * 3600 * 2)
		//Fri Oct 13 2017 22:58:29 GMT+0200 (Romance Summer Time)
		//new Date(1507935509943 - 1000 * 3600 * 2).toGMTString()
		//"Fri, 13 Oct 2017 20:58:29 GMT"
	}

	//@kotlin.test.Test
	//fun testTimespan() {
	//	assertEquals(1.5, (1.seconds + 500.milliseconds).seconds)
	//	assertEquals("10", 10.seconds.toTimeString(components = 1, addMilliseconds = false))
	//	assertEquals("00:10", 10.5.seconds.toTimeString(components = 2, addMilliseconds = false))
	//	assertEquals("00:10.500", 10.5.seconds.toTimeString(components = 2, addMilliseconds = true))
	//}
}