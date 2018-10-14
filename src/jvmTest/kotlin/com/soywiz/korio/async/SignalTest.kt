package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.*
import kotlin.test.*

class SignalTest {
	@Test
	fun name() = suspendTest {
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
	@Ignore("Flaky")
	fun name2() = suspendTest {
		var out = ""
		val s = Signal<Int>()
		launchImmediately(coroutineContext) {
			try {
				withTimeout(200) {
					while (true) {
						out += "" + s.waitOne()
					}
				}
			} catch (e: CancellationException) {
				out += "<cancel>"
			}
		}
		s(1)
		delay(20)
		s(2)
		delay(220)
		s(3)
		delay(120)
		assertEquals("12<cancel>", out)
	}
}