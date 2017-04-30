package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test

class AsyncQueueTest {
	@Test
	fun sequence() = syncTest {
		var out = ""
		val queue = AsyncQueue()
		queue { sleep(100); out += "a" }
		queue { sleep(100); out += "b" }
		step(0)
		Assert.assertEquals("", out)
		step(100)
		Assert.assertEquals("a", out)
		step(100)
		Assert.assertEquals("ab", out)
	}

	@Test
	fun parallel() = syncTest {
		var out = ""
		val queue1 = AsyncQueue()
		val queue2 = AsyncQueue()
		queue1 { sleep(100); out += "a" }
		queue2 { sleep(100); out += "b" }
		step(0)
		Assert.assertEquals("", out)
		step(100)
		Assert.assertEquals("ab", out)
		step(100)
		Assert.assertEquals("ab", out)
	}
}