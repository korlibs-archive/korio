package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CancellationException

class PromiseTest {
	@Test
	fun name() = sync {
		var out = ""
		val p = spawn {
			try {
				sleep(100)
				10
			} catch (e: CancellationException) {
				out += "|"
				throw e
			}
		}

		p.cancel()

		try {
			out += "" + p.await()
		} catch(e: Throwable) {
			out += e.javaClass.name
		}

		Assert.assertEquals(
			"java.util.concurrent.CancellationException",
			out
		)
	}
}