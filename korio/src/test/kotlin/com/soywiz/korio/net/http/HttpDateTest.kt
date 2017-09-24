package com.soywiz.korio.net.http

import org.junit.Test
import kotlin.test.assertEquals

class HttpDateTest {
	@Test
	fun name() {
		assertEquals("Mon, 18 Sep 2017 11:58:45 GMT", HttpDate.format(1505779125916L))
	}

	@Test
	fun reversible() {
		val STR = "Tue, 19 Sep 2017 00:58:45 GMT"

		assertEquals(STR, HttpDate.format(HttpDate.parse(STR)))
	}
}