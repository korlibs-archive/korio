package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import org.junit.Test
import kotlin.test.assertEquals

class MemoryVfsTest {
	@Test
	fun name() = syncTest {
		val log = arrayListOf<String>()
		val mem = MemoryVfs()

		mem.watch {
			log += it.toString()
		}.use {
			mem["item.txt"].writeString("test")
			mem["test"].mkdir()
			mem["test"].delete()
			this.step(100)
			assertEquals(
				"[MODIFIED(NodeVfs[/item.txt]), CREATED(NodeVfs[/test]), DELETED(NodeVfs[/test])]",
				log.toString()
			)
		}
	}
}