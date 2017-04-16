package com.soywiz.korio.serialization.xml

import org.junit.Assert
import org.junit.Test

class XmlTest {
	@Test
	fun name() {
		val xml = Xml("<hello a=\"10\" Zz='20'><demo c='7' /></hello>")
		Assert.assertEquals(10, xml.int("a"))
		Assert.assertEquals(10, xml.int("A"))
		Assert.assertEquals(20, xml.int("zZ"))
		Assert.assertEquals("hello", xml.name)
		Assert.assertEquals(7, xml["demo"].first().int("c"))
		Assert.assertEquals(7, xml["Demo"].first().int("c"))
		Assert.assertEquals("""<hello a="10" Zz="20"><demo c="7"/></hello>""", xml.toString())
	}

	@Test
	fun name2() {
		val xml = Xml("<a_b />")
	}
}