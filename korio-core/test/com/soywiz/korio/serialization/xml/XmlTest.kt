package com.soywiz.korio.serialization.xml

import org.junit.Assert
import org.junit.Test

class XmlTest {
	@Test
	fun name() {
		val xml = Xml("<hello a=\"10\"><demo c='7' /></hello>")
		Assert.assertEquals("""<hello a="10"><demo c="7"/></hello>""", xml.toString())
		Assert.assertEquals(10, xml.int("a"))
		Assert.assertEquals("hello", xml.name)
		Assert.assertEquals(7, xml["demo"].first().int("c"))
	}
}