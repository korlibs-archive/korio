package com.soywiz.korio.vfs

import com.soywiz.korio.async.sync
import com.soywiz.korio.expectException
import org.junit.Assert
import org.junit.Test
import java.io.FileNotFoundException

class MountableVfsTest {
	@Test
	fun testMountable() = sync {
		val root = MountableVfs({
			mount("/zip/demo2", ResourcesVfs["hello.zip"].openAsZip())
			mount("/zip", ResourcesVfs["hello.zip"].openAsZip())
			mount("/zip/demo", ResourcesVfs["hello.zip"].openAsZip())
			mount("/iso", ResourcesVfs["isotest.iso"].openAsIso())
		})
		Assert.assertEquals("HELLO WORLD!", root["/zip/hello/world.txt"].readString())
		Assert.assertEquals("HELLO WORLD!", root["/zip/demo/hello/world.txt"].readString())
		Assert.assertEquals("HELLO WORLD!", root["/zip/demo2/hello/world.txt"].readString())
		Assert.assertEquals("WORLD!", root["iso"]["hello/world.txt"].readString())

		(root.vfs as Mountable).unmount("/zip")

		expectException<FileNotFoundException> {
			root["/zip/hello/world.txt"].readString()
		}
	}
}