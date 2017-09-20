package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.toList
import org.junit.Test
import kotlin.test.assertEquals

class IsoVfsTest {
	@Test
	fun testIso() = syncTest {
		val isotestIso = ResourcesVfs["isotest.iso"].openAsIso()
		assertEquals(
			listOf("/HELLO", "/HELLO/WORLD.TXT"),
			isotestIso.listRecursive().toList().map { it.fullname }
		)

		// Case insensitive!
		assertEquals(
			"WORLD!",
			isotestIso["hello"]["world.txt"].readString()
		)
	}
}