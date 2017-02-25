package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CancellationException

class SignalTest {
	@Test
	fun name() = sync(EventLoopTest()) {
		var out = ""
		val s = Signal<Int>()
		Assert.assertEquals(0, s.listenerCount)
		val c1 = s.add { out += "[$it]"  }
		Assert.assertEquals(1, s.listenerCount)
		s(1)
		val c2 = s.add { out += "{$it}"  }
		Assert.assertEquals(2, s.listenerCount)
		s(2)
		s.once { out += "<$it>"  }
		Assert.assertEquals(3, s.listenerCount)
		s(3)
		Assert.assertEquals(2, s.listenerCount)
		c2.close()
		Assert.assertEquals(1, s.listenerCount)
		s(4)
		c1.close()
		Assert.assertEquals(0, s.listenerCount)
		s(5)
		Assert.assertEquals(0, s.listenerCount)
		Assert.assertEquals("[1][2]{2}[3]{3}<3>[4]", out)
	}

	@Test
	fun name2() = syncTest {
		var out = ""
		val s = Signal<Int>()
		spawn {
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
		Assert.assertEquals("12<cancel>", out)
	}
}