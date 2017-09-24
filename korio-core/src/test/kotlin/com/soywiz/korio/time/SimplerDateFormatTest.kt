package com.soywiz.korio.time

import org.junit.Test
import kotlin.test.assertEquals

class SimplerDateFormatTest {
	// Sun, 06 Nov 1994 08:49:37 GMT
	val format = SimplerDateFormat("EEE, dd MMM yyyy HH:mm:ss z")

	@Test
	fun testParse() {
		assertEquals(784111777000L, format.parse("Sun, 06 Nov 1994 08:49:37 GMT"))
	}

	@Test
	fun testFormat() {
		assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", format.format(784111777000L))
	}

	@Test
	fun testParseFormat() {
		assertEquals("Sun, 06 Nov 1994 08:49:37 GMT", format.format(format.parse("Sun, 06 Nov 1994 08:49:37 GMT")))
	}
}