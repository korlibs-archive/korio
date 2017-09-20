package com.soywiz.korio.net.http

import org.junit.Test
import kotlin.test.assertEquals

class HttpDateTest {
	@Test
	fun name() {
		assertEquals(
			"Mon, 18 Sep 2017 23:58:45 GMT",
			HttpDate.format(1505779125916L)
		)
	}
}