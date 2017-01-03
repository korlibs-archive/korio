package com.soywiz.korio.vfs

import com.soywiz.korio.async.sync
import com.soywiz.korio.async.toList
import org.junit.Assert
import org.junit.Test

class IsoVfsTest {
	@Test
	fun testIso() = sync {
		val isotestIso = ResourcesVfs["isotest.iso"].openAsIso()
		Assert.assertEquals(
			listOf("/HELLO", "/HELLO/WORLD.TXT"),
			isotestIso.listRecursive().toList().map { it.fullname }
		)

		// Case insensitive!
		Assert.assertEquals(
			"WORLD!",
			isotestIso["hello"]["world.txt"].readString()
		)
	}
}