package com.soywiz.korio.async

import org.junit.Test
import java.util.concurrent.CancellationException
import kotlin.test.assertEquals

class SignalTest {
	@Test
	fun name() = syncTest {
		var out = ""
		val s = Signal<Int>()
		assertEquals(0, s.listenerCount)
		val c1 = s.add { out += "[$it]" }
		assertEquals(1, s.listenerCount)
		s(1)
		val c2 = s.add { out += "{$it}" }
		assertEquals(2, s.listenerCount)
		s(2)
		s.once { out += "<$it>" }
		assertEquals(3, s.listenerCount)
		s(3)
		assertEquals(2, s.listenerCount)
		c2.close()
		assertEquals(1, s.listenerCount)
		s(4)
		c1.close()
		assertEquals(0, s.listenerCount)
		s(5)
		assertEquals(0, s.listenerCount)
		assertEquals("[1][2]{2}[3]{3}<3>[4]", out)
	}

	@Test
	fun name2() = syncTest {
		var out = ""
		val s = Signal<Int>()
		spawn(coroutineContext) {
			try {
				withTimeout(100) {
					while (true) {
						out += "" + s.waitOne()
					}
				}
			} catch (e: CancellationException) {
				out += "<cancel>"
			}
		}
		s(1)
		sleep(50)
		s(2)
		sleep(100)
		s(3)
		sleep(100)
		assertEquals("12<cancel>", out)
	}
}