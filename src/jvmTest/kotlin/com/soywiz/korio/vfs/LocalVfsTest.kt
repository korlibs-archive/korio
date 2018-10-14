package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.file.std.*
import java.io.*
import kotlin.test.*

class LocalVfsTest {
	val temp = TempVfs()

	@Test
	fun name() = suspendTest {
		val content = "HELLO WORLD!"
		temp["korio.temp"].writeString(content)
		temp["korio.temp2"].writeFile(temp["korio.temp"])
		temp["korio.temp3"].writeFile(temp["korio.temp"])
		temp["korio.temp3"].writeStream(temp["korio.temp"].open().slice(0 until 3))
		assertEquals(content, temp["korio.temp2"].readString())
		assertEquals("HEL", temp["korio.temp3"].readString())
		assertEquals(true, temp["korio.temp"].delete())
		assertEquals(true, temp["korio.temp2"].delete())
		assertEquals(true, temp["korio.temp3"].delete())
		assertEquals(false, temp["korio.temp3"].delete())
		assertEquals(
			File(System.getProperty("java.io.tmpdir"), "korio.temp3").absolutePath.replace('\\', '/'),
			temp["korio.temp3"].absolutePath
		)
		assertEquals("1", temp.execToString(listOf("echo", "1")).trim())
		//assertEquals("1", temp.execToString(listOf("pwd")).trim())
		Unit
	}

	@Test
	fun ensureParent() = suspendTest {
		temp["korio.temp.folder/test.txt"].ensureParents().writeString("HELLO")
		temp["korio.temp.folder/test.txt"].delete()
		temp["korio.temp.folder"].delete()
		Unit
	}
}