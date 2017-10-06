package com.soywiz.korio.ext.web.sstatic

import com.soywiz.korio.async.syncTest
import org.junit.Test
import kotlin.test.assertEquals

class StaticServeTest {
	@Test
	fun parseRange() = syncTest {
		assertEquals(listOf(9500L..9999L), StaticServe.parseRange("bytes=-500", 10000L))
		assertEquals(listOf(9500L..9999L), StaticServe.parseRange("bytes=9500-", 10000L))
		assertEquals(listOf(0L..0L, 9999L..9999L), StaticServe.parseRange("bytes=0-0,-1", 10000L))
	}
}