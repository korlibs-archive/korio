package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.toList
import com.soywiz.korio.expectException
import com.soywiz.korio.stream.openAsync
import org.junit.Test
import java.io.FileNotFoundException
import kotlin.test.assertEquals

class JailVfsTest {
	@Test
	fun name() = syncTest {
		val mem = MemoryVfsMix(
			"hello/secret.txt" to "SECRET!",
			"hello/world/test.txt" to "HELLO WORLD!"
		)

		assertEquals(
			"[/hello, /hello/secret.txt, /hello/world, /hello/world/test.txt]",
			mem.listRecursive().toList().map { it.fullname }.toString()
		)

		val worldFolder = mem["hello/world"]
		val worldFolderJail = mem["hello/world"].jail()

		assertEquals(
			"SECRET!",
			worldFolder["../secret.txt"].readString()
		)

		expectException<FileNotFoundException> {
			worldFolderJail["../secret.txt"].readString()
		}
	}
}