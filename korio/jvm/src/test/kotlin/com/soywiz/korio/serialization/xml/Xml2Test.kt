package com.soywiz.korio.serialization.xml

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test
import kotlin.test.assertEquals

class Xml2Test {
	@Test
	fun name2() = syncTest {
		val xml = ResourcesVfs["test.xml"].readXml()
		assertEquals("test", xml.name)
		assertEquals("hello", xml.text)
	}
}