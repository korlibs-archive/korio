package com.soywiz.korio.vfs

import com.soywiz.korio.async.sync
import com.soywiz.korio.stream.slice
import org.junit.Assert
import org.junit.Test

class LocalVfsTest {
	@Test
	fun name() = sync {
		val temp = TempVfs()
		val content = "HELLO WORLD!"
		temp["korio.temp"].writeString(content)
		temp["korio.temp2"].writeFile(temp["korio.temp"])
		temp["korio.temp3"].writeFile(temp["korio.temp"])
		temp["korio.temp3"].writeStream(temp["korio.temp"].open().slice(0 until 3))
		Assert.assertEquals(content, temp["korio.temp2"].readString())
		Assert.assertEquals("HEL", temp["korio.temp3"].readString())
		Assert.assertEquals(true, temp["korio.temp"].delete())
		Assert.assertEquals(true, temp["korio.temp2"].delete())
		Assert.assertEquals(true, temp["korio.temp3"].delete())
		Assert.assertEquals(false, temp["korio.temp3"].delete())
		Unit
	}
}