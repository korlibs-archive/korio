package com.soywiz.korio.serialization.xml

import org.junit.Assert
import org.junit.Test

class XmlTest {
	@Test
	fun name() {
		val xml = Xml2("<hello a=10><demo c=7 /></hello>")
		Assert.assertEquals(10, xml.int("a"))
		Assert.assertEquals("hello", xml.name)
		Assert.assertEquals(7, xml["demo"].first().int("c"))
	}
}