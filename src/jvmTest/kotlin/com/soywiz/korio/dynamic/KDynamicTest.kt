package com.soywiz.korio.dynamic

import org.junit.Test
import kotlin.test.*

class KDynamicTest {
	@Test
	fun test() {
		assertEquals("10", KDynamic { global["java.lang.String"].dynamicInvoke("valueOf", 10) })
		assertEquals("10", KDynamic { global["java"]["lang.String"].dynamicInvoke("valueOf", 10) })
		assertEquals("HELLO", KDynamic { "hello".dynamicInvoke("toUpperCase") })
	}
}