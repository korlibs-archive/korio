package com.soywiz.korio.vfs

import com.soywiz.korio.KorioNative.syncTest
import org.junit.Test

class CommonResourcesVfsTest {
	@Test
	fun testCanReadResourceProperly() = syncTest {
		//assertEquals("HELLO", resourcesVfs["resource.txt"].readString())
	}
}