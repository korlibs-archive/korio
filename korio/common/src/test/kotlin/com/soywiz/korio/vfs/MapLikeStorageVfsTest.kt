package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.toList
import com.soywiz.korio.lang.toByteArray
import org.junit.Test
import kotlin.test.assertEquals

class MapLikeStorageVfsTest {
	@Test
	fun name() = syncTest {
		val map = LinkedHashMap<String, String>()

		val root = MapLikeStorageVfs(object : SimpleStorage {
			override suspend fun get(key: String): String? = map[key]
			override suspend fun set(key: String, value: String) = run { map[key] = value }
			override suspend fun remove(key: String): Unit = run { map.remove(key) }
		}).root

		assertEquals(listOf(), root.list().toList())
		println(map)
		root["demo.txt"].writeBytes("hello".toByteArray())
		assertEquals(listOf("/demo.txt"), root.list().toList().map { it.fullname })
		println(map)
		assertEquals("hello", root["demo.txt"].readString())
		root["demo"].mkdir()
		assertEquals(listOf("/demo.txt", "/demo"), root.list().toList().map { it.fullname })
		root["demo/hello/world/yay"].mkdir()
		root["demo/hello/world/yay/file.txt"].writeString("DEMO")

		assertEquals(
			"[/demo.txt, /demo, /demo/hello, /demo/hello/world, /demo/hello/world/yay, /demo/hello/world/yay/file.txt]",
			root.listRecursive().toList().map { it.fullname }.toString()
		)

		assertEquals(true, root["demo.txt"].exists())
		assertEquals(5, root["demo.txt"].size())

		assertEquals(false, root["unexistant"].exists())
	}
}