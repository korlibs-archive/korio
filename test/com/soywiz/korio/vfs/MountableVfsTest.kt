package com.soywiz.korio.vfs

import com.soywiz.korio.async.sync
import org.junit.Assert
import org.junit.Test

class MountableVfsTest {
	@Test
	fun testMountable() = sync {
		val resources = ResourcesVfs()
		val root = MountableVfs({
			mount("/zip/demo2", resources["hello.zip"].openAsZip())
			mount("/zip", resources["hello.zip"].openAsZip())
			mount("/zip/demo", resources["hello.zip"].openAsZip())
			mount("/iso", resources["isotest.iso"].openAsIso())
		})
		Assert.assertEquals("HELLO WORLD!", root["/zip/hello/world.txt"].readString())
		Assert.assertEquals("HELLO WORLD!", root["/zip/demo/hello/world.txt"].readString())
		Assert.assertEquals("HELLO WORLD!", root["/zip/demo2/hello/world.txt"].readString())
		Assert.assertEquals("WORLD!", root["iso"]["hello/world.txt"].readString())
	}
}