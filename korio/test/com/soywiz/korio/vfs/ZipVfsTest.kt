package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.toList
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAvailable
import org.junit.Assert
import org.junit.Test
import java.text.SimpleDateFormat

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class ZipVfsTest {
	@Test
	fun testZipUncompressed() = syncTest {
		val helloZip = ResourcesVfs["hello.zip"].openAsZip()

		Assert.assertEquals(
				"[VfsStat(file=ZipVfs(ResourcesVfs[/hello.zip])[/hello], exists=true, isDirectory=true, size=0, device=-1, inode=0, mode=511, owner=nobody, group=nobody, createTime=1482776692000, modifiedTime=1482776692000, lastAccessTime=1482776692000, extraInfo=null)]",
				helloZip.list().toList().map { it.stat() }.toString()
		)

		Assert.assertEquals(
				"[VfsStat(file=ZipVfs(ResourcesVfs[/hello.zip])[/hello/world.txt], exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=1482776692000, modifiedTime=1482776692000, lastAccessTime=1482776692000, extraInfo=null)]",
				helloZip["hello"].list().toList().map { it.stat() }.toString()
		)

		Assert.assertEquals(
				"VfsStat(file=ZipVfs(ResourcesVfs[/hello.zip])[/hello/world.txt], exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=1482776692000, modifiedTime=1482776692000, lastAccessTime=1482776692000, extraInfo=null)",
				helloZip["hello/world.txt"].stat().toString()
		)
		Assert.assertEquals(
				"HELLO WORLD!",
				helloZip["hello/world.txt"].readString()
		)

		Assert.assertEquals(
				"2016-12-26 18:24:52",
				SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(helloZip["hello/world.txt"].stat().createDate)
		)
	}

	@Test
	fun testZipCompressed() = syncTest {
		val helloZip = ResourcesVfs["compressedHello.zip"].openAsZip()

		val contents = "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD!"

		Assert.assertEquals(
				contents,
				helloZip["hello/compressedWorld.txt"].readString()
		)

		Assert.assertEquals(
				contents,
				helloZip["hello/compressedWorld.txt"].openUse { readAvailable() }.toString(Charsets.UTF_8)
		)

		Assert.assertEquals(
				contents.toByteArray().size.toLong(),
				helloZip["hello/compressedWorld.txt"].openUse { getLength() }
		)

		Assert.assertEquals(
				"[/hello, /hello/compressedWorld.txt, /hello/world.txt]",
				helloZip.listRecursive().toList().map { it.fullname }.toString()
		)
	}

	@Test
	fun testCreateZip() = syncTest {
		val mem = MemoryVfsMix(
			"/test.txt" to "test",
			"/hello/world.txt" to "hello world world world world!"
		)
		val zipBytes = mem.treeCreateZip()
		//zipBytes.writeToFile("c:/temp/mytest.zip")
		val zip = zipBytes.openAsync().openAsZip()
		Assert.assertEquals(
			"test",
			zip["/test.txt"].readString()
		)
		Assert.assertEquals(
			"hello world world world world!",
			zip["/hello/world.txt"].readString()
		)
	}
}
