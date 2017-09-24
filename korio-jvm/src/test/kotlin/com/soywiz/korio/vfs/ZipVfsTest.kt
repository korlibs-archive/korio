package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.toList
import com.soywiz.korio.serialization.xml.readXml
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAvailable
import com.soywiz.korio.time.SimpleDateFormat
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class ZipVfsTest {
	@Test
	fun testZipUncompressed() = syncTest {
		val helloZip = ResourcesVfs["hello.zip"].openAsZip()

		assertEquals(
			"[VfsStat(file=ZipVfs(ResourcesVfs[/hello.zip])[/hello], exists=true, isDirectory=true, size=0, device=-1, inode=0, mode=511, owner=nobody, group=nobody, createTime=1482776692000, modifiedTime=1482776692000, lastAccessTime=1482776692000, extraInfo=null)]",
			helloZip.list().toList().map { it.stat() }.toString()
		)

		assertEquals(
			"[VfsStat(file=ZipVfs(ResourcesVfs[/hello.zip])[/hello/world.txt], exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=1482776692000, modifiedTime=1482776692000, lastAccessTime=1482776692000, extraInfo=null)]",
			helloZip["hello"].list().toList().map { it.stat() }.toString()
		)

		assertEquals(
			"VfsStat(file=ZipVfs(ResourcesVfs[/hello.zip])[/hello/world.txt], exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=1482776692000, modifiedTime=1482776692000, lastAccessTime=1482776692000, extraInfo=null)",
			helloZip["hello/world.txt"].stat().toString()
		)
		assertEquals(
			"HELLO WORLD!",
			helloZip["hello/world.txt"].readString()
		)

		assertEquals(
			"2016-12-26 18:24:52",
			SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(helloZip["hello/world.txt"].stat().createDate)
		)
	}

	@Test
	fun testZipCompressed() = syncTest {
		val helloZip = ResourcesVfs["compressedHello.zip"].openAsZip()

		val contents = "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD!"

		assertEquals(
			contents,
			helloZip["hello/compressedWorld.txt"].readString()
		)

		assertEquals(
			contents,
			helloZip["hello/compressedWorld.txt"].openUse { readAvailable() }.toString(Charsets.UTF_8)
		)

		assertEquals(
			contents.toByteArray().size.toLong(),
			helloZip["hello/compressedWorld.txt"].openUse { getLength() }
		)

		assertEquals(
			"[/hello, /hello/compressedWorld.txt, /hello/world.txt]",
			helloZip.listRecursive().toList().map { it.fullname }.toString()
		)

		println(helloZip.stat())
		assertEquals(true, helloZip.exists())
		assertEquals(true, helloZip.isDirectory())
		assertEquals(true, helloZip["/"].isDirectory())
		val mem = MemoryVfs()
		helloZip.copyToTree(mem)
		assertEquals(contents, mem["hello/compressedWorld.txt"].readString())

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
		assertEquals(
			"test",
			zip["/test.txt"].readString()
		)
		assertEquals(
			"hello world world world world!",
			zip["/hello/world.txt"].readString()
		)
	}

	@Test
	@Ignore
	fun testZip1() = syncTest {
		val mem = MemoryVfs()
		//UrlVfs("https://github.com/soywiz/korge-tools/releases/download/binaries/rhubarb-lip-sync-1.4.2-win32.zip").copyTo(LocalVfs["c:/temp/file.zip"])

		//val zip = LocalVfs("c:/temp/rhubarb-lip-sync-1.4.2-osx.zip").openAsZip()
		val zip = LocalVfs("c:/temp/rhubarb-lip-sync-1.4.2-win32.zip").openAsZip()
		//zip.copyTo(mem) // IOException
		zip.copyToTree(mem) // IOException

		//assertEquals(
		//	listOf("/rhubarb-lip-sync-1.4.2-osx"),
		//	zip.list().map { it.fullname }.toList()
		//)
		//val mem = MemoryVfs()
		//zip.copyToTree(mem)
	}

	@Test
	fun testReadChunk() = syncTest {
		val zip = ResourcesVfs["simple1.fla.zip"].openAsZip()
		val xml = zip["DOMDocument.xml"].readXml()
		assertEquals(1, xml.descendants.filter { it.nameLC == "frames" }.count())
	}
}
