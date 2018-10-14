package com.soywiz.korio.vfs

import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import java.io.FileNotFoundException
import kotlin.test.*

class MountableVfsTest {
	@Test
	fun testMountable() = suspendTest {
		val root = MountableVfs({
			mount("/zip/demo2", ResourcesVfs["hello.zip"].openAsZip())
			mount("/zip", ResourcesVfs["hello.zip"].openAsZip())
			mount("/zip/demo", ResourcesVfs["hello.zip"].openAsZip())
			mount("/iso", ResourcesVfs["isotest.iso"].openAsIso())
		})
		assertEquals("HELLO WORLD!", root["/zip/hello/world.txt"].readString())
		assertEquals("HELLO WORLD!", root["/zip/demo/hello/world.txt"].readString())
		assertEquals("HELLO WORLD!", root["/zip/demo2/hello/world.txt"].readString())
		assertEquals("WORLD!", root["iso"]["hello/world.txt"].readString())

		(root.vfs as Mountable).unmount("/zip")

		expectException<FileNotFoundException> {
			root["/zip/hello/world.txt"].readString()
		}
	}
}