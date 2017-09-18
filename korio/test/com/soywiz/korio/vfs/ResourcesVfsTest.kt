package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import org.junit.Assert
import org.junit.Test

class ResourcesVfsTest {
	@Test
	fun name() = syncTest {
		for (v in ResourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList()) {
			println(v)
		}

		Assert.assertEquals(
			"[a.txt, b.txt]",
			ResourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList().map { it.basename }.sorted().toString()
		)
	}
}