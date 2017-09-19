package com.soywiz.korio.net.http

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class HttpDateTest {
	@Test
	fun name() {
		assertEquals(
			"Mon, 18 Sep 2017 23:58:45 GMT",
			HttpDate.format(Date(1505779125916L))
		)
	}
}