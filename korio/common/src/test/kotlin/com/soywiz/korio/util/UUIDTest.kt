package com.soywiz.korio.util

import org.junit.Test
import kotlin.test.assertEquals

class UUIDTest {
	@Test
	fun name() {
		assertEquals("00000000-0000-0000-0000-000000000000", UUID("00000000-0000-0000-0000-000000000000").toString())
		assertEquals("00000000-0000-0000-0000-000000000000", UUID.randomUUID().toString())
	}
}