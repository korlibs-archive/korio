package com.soywiz.korio.async

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CancellationException

class SignalTest {
	@Test
	fun name() = sync(EventLoopTest()) {
		var out = ""
		val s = Signal<Int>()
		val c1 = s.add { out += "[$it]"  }
		s(1)
		val c2 = s.add { out += "{$it}"  }
		s(2)
		s.once { out += "<$it>"  }
		s(3)
		c2.close()
		s(4)
		c1.close()
		s(5)
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