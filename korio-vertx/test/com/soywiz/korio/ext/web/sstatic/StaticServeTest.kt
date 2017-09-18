package com.soywiz.korio.ext.web.sstatic

import com.soywiz.korio.async.syncTest
import org.junit.Assert
import org.junit.Test

class StaticServeTest {
	@Test
	fun parseRange() = syncTest {
		Assert.assertEquals(listOf(9500L..9999L), StaticServe.parseRange("bytes=-500", 10000L))
		Assert.assertEquals(listOf(9500L..9999L), StaticServe.parseRange("bytes=9500-", 10000L))
		Assert.assertEquals(listOf(0L..0L, 9999L..9999L), StaticServe.parseRange("bytes=0-0,-1", 10000L))

	}
}