package com.soywiz.korio.async

import com.soywiz.korio.expectException
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CancellationException

class PromiseTest {
	@Test
	fun nullPromise() = syncTest {
		val promise = Promise.resolved<String?>(null)
		Assert.assertEquals(null, promise.await())
	}

	@Test
	fun rejectedPromise() = syncTest {
		val promise = Promise.rejected<String?>(IllegalStateException())
		expectException<IllegalStateException> { promise.await() }
	}

	@Test
	fun name() = syncTest {
		var out = ""
		val p = spawn(coroutineContext) {
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
			out += e::class.java.name
		}

		Assert.assertEquals(
			"java.util.concurrent.CancellationException",
			out
		)
	}

	@Test
	@Ignore
	fun cancel1() = syncTest {
		var out = ""
		val prom = go {
			out += "a"
			sleep(1000)
			out += "b"
			go {
				sleep(1000)
				out += "c"
				sleep(1000)
				out += "d"
			}.await()
		}
		Assert.assertEquals("a", out)
		this@syncTest.step(1200)
		Assert.assertEquals("ab", out)
		this@syncTest.step(100)
		Assert.assertEquals("ab", out)
		prom.cancel()
		this@syncTest.step(1200)
		Assert.assertEquals("ab", out)
	}

	@Test
	@Ignore
	fun jvmUnsafeAwait() = syncTest {
		Assert.assertEquals(10, async(coroutineContext) {
			sleep(20)
			10
		}.jvmSyncAwait())
	}
}