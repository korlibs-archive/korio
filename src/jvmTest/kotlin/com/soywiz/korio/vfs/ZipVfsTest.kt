package com.soywiz.korio.vfs

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.stream.*
import kotlin.test.*

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class ZipVfsTest {
	@Test
	fun testZipUncompressed1() = suspendTest {
		val helloZip = ResourcesVfs["hello.zip"].openAsZip()

		assertEquals(
			"[VfsStat(file=/hello, exists=true, isDirectory=true, size=0, device=-1, inode=0, mode=511, owner=nobody, group=nobody, createTime=Mon, 26 Dec 2016 00:00:10 UTC, modifiedTime=Thu, 01 Jan 1970 00:00:00 UTC, lastAccessTime=Thu, 01 Jan 1970 00:00:00 UTC, extraInfo=null, id=null)]",
			helloZip.list().toList().map { it.stat().toString(showFile = false) }.toString()
		)
	}

	@Test
	fun testZipUncompressed2() = suspendTest {
		val helloZip = ResourcesVfs["hello.zip"].openAsZip()

		assertEquals(
			"[VfsStat(file=/hello/world.txt, exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=Mon, 26 Dec 2016 00:00:10 UTC, modifiedTime=Thu, 01 Jan 1970 00:00:00 UTC, lastAccessTime=Thu, 01 Jan 1970 00:00:00 UTC, extraInfo=null, id=null)]",
			helloZip["hello"].list().toList().map { it.stat().toString(showFile = false) }.toString()
		)
	}

	@Test
	fun testZipUncompressed3() = suspendTest {
		val helloZip = ResourcesVfs["hello.zip"].openAsZip()

		assertEquals(
			"VfsStat(file=/hello/world.txt, exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=Mon, 26 Dec 2016 00:00:10 UTC, modifiedTime=Thu, 01 Jan 1970 00:00:00 UTC, lastAccessTime=Thu, 01 Jan 1970 00:00:00 UTC, extraInfo=null, id=null)",
			helloZip["hello/world.txt"].stat().toString(showFile = false)
		)
	}

	@Test
	fun testZipUncompressed4() = suspendTest {
		val helloZip = ResourcesVfs["hello.zip"].openAsZip()

		assertEquals(
			"HELLO WORLD!",
			helloZip["hello/world.txt"].readString()
		)
	}

	@Test
	fun testZipUncompressed5() = suspendTest {
		val helloZip = ResourcesVfs["hello.zip"].openAsZip()

		val stat = helloZip["hello/world.txt"].stat()
		val createTime = stat.createTime

		assertEquals(
			"2016-12-26 00:00:10",
			DateFormat("YYYY-MM-dd HH:mm:ss").format(createTime)
		)
	}

	@Test
	fun testZipCompressed() = suspendTest {
		val helloZip = ResourcesVfs["compressedHello.zip"].openAsZip()

		val contents =
			"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD!"

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
			helloZip.listRecursive().toList().map { it.fullName }.toString()
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
	fun testCreateZip() = suspendTest {
		val mem = MemoryVfsMix(
			"/test.txt" to "test",
			"/hello/world.txt" to "hello world world world world!"
		)
		val zipBytes = mem.createZipFromTree()
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
	fun testZip1() = suspendTest {
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
	fun testReadChunk() = suspendTest {
		val zip = ResourcesVfs["simple1.fla.zip"].openAsZip()
		val xml = zip["DOMDocument.xml"].readXml()
		assertEquals(1, xml.descendants.filter { it.nameLC == "frames" }.count())
	}
}
