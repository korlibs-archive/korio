package com.soywiz.korio.serialization.xml

import com.soywiz.korio.async.sync
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Assert
import org.junit.Test

class Xml2Test {
	@Test
	fun name2() = syncTest {
		val xml = ResourcesVfs["test.xml"].readXml()
		Assert.assertEquals("test", xml.name)
		Assert.assertEquals("hello", xml.text)
	}
}