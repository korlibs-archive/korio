package com.soywiz.korio.serialization.xml

import com.soywiz.korio.ds.lmapOf
import org.junit.Test
import kotlin.test.assertEquals

class XmlTest {
	@Test
	fun name() {
		val xml = Xml("<hello a=\"10\" Zz='20'><demo c='7' /></hello>")
		assertEquals(10, xml.int("a"))
		assertEquals(10, xml.int("A"))
		assertEquals(20, xml.int("zZ"))
		assertEquals("hello", xml.name)
		assertEquals(7, xml["demo"].first().int("c"))
		assertEquals(7, xml["Demo"].first().int("c"))
		assertEquals("""<hello a="10" Zz="20"><demo c="7"/></hello>""", xml.toString())
	}

	@Test
	fun name2() {
		val xml = Xml("<a_b />")
	}

	@Test
	fun name3() {
		assertEquals("""<test z="1" b="2"/>""", Xml.Tag("test", lmapOf("z" to 1, "b" to 2), listOf()).outerXml)

	}

}