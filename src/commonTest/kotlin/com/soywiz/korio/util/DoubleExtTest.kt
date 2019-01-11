package com.soywiz.korio.util

import kotlin.test.*

class DoubleExtTest {
	@Test
	fun test() {
		assertEquals("10", 10.0.toString(0))
		assertEquals("10.0", 10.0.toString(1))
		assertEquals("10.00", 10.0.toString(2))
		assertEquals("10.000", 10.0.toString(3))

		assertEquals("10", 10.0.toString(0, true))
		assertEquals("10", 10.0.toString(1, true))
		assertEquals("10", 10.0.toString(2, true))
	}

	@Test
	fun test2() {
		assertEquals("10", 10.123.toString(0))
		assertEquals("10.1", 10.123.toString(1))
		assertEquals("10.12", 10.123.toString(2))
		assertEquals("10.123", 10.123.toString(3))
		assertEquals("10.1230", 10.123.toString(4))

		assertEquals("10", 10.123.toString(0, true))
		assertEquals("10.1", 10.123.toString(1, true))
		assertEquals("10.12", 10.123.toString(2, true))
		assertEquals("10.123", 10.123.toString(3, true))
		assertEquals("10.123", 10.123.toString(4, true))
	}

	@Test
	fun test3() {
		//assertEquals("1.0e21", 10e20.toString().toLowerCase())

		assertEquals("100000000000000000000", 1e20.toString(0))
		assertEquals("1000000000000000000000", 1e21.toString(0))
		assertEquals("10000000000000000000000", 1e22.toString(0))
		assertEquals("123000000000000000000", 1.23e20.toString(0))
		assertEquals("1230000000000000000000", 1.23e21.toString(0))

		assertEquals("0", 1.23e-3.toString(0))
		assertEquals("0.0", 1.23e-3.toString(1))
		assertEquals("0.00123", 1.23e-3.toString(10, true))
		assertEquals("0.000123", 1.23e-4.toString(10, true))
		assertEquals("0.0000123", 1.23e-5.toString(10, true))
		assertEquals("0.00000123", 1.23e-6.toString(10, true))
	}
}