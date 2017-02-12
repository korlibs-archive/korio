package com.soywiz.korio.vfs

import com.soywiz.korio.async.map
import com.soywiz.korio.async.sync
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.toList
import com.soywiz.korio.stream.openAsync
import org.junit.Assert
import org.junit.Test

class CopyToTreeTest {
	@Test
	fun name() = syncTest {
		val mem = MemoryVfs(mapOf(
			"root.txt" to "hello".toByteArray().openAsync(),
			"hello/world.txt" to "hello".toByteArray().openAsync()
		))
		val out = MemoryVfs()
		mem.copyToTree(out)
		Assert.assertEquals(
			"[/root.txt, /hello, /hello/world.txt]",
			out.listRecursive().map { it.fullname }.toList().toString()
		)
	}
}