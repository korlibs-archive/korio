package com.soywiz.korio.vfs

import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.filter
import com.soywiz.korio.async.sync
import com.soywiz.korio.async.toList
import org.junit.Assert
import org.junit.Test

class ResourcesVfsTest {
	@Test
	fun name() = sync(EventLoopTest()) {
		for (v in ResourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList()) {
			println(v)
		}

		Assert.assertEquals(
			"[a.txt, b.txt]",
			ResourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList().map { it.basename }.toString()
		)
	}
}