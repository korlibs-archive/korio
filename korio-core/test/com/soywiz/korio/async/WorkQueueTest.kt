package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test

class WorkQueueTest {
	@Test
	fun sequence() = sync(EventLoopTest()) {
		var out = ""
		val queue = WorkQueue()
		queue { sleep(100); out += "a" }
		queue { sleep(100); out += "b" }
		Assert.assertEquals("", out)
		step(100)
		Assert.assertEquals("a", out)
		step(100)
		Assert.assertEquals("ab", out)
	}

	@Test
	fun parallel() = sync(EventLoopTest()) {
		var out = ""
		val queue1 = WorkQueue()
		val queue2 = WorkQueue()
		queue1 { sleep(100); out += "a" }
		queue2 { sleep(100); out += "b" }
		Assert.assertEquals("", out)
		step(100)
		Assert.assertEquals("ab", out)
		step(100)
		Assert.assertEquals("ab", out)
	}
}