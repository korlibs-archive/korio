package com.soywiz.korio.vfs

import com.soywiz.korio.async.sync
import org.junit.Assert
import org.junit.Test

class LocalVfsTest {
	@Test
	fun name() = sync {
		val temp = TempVfs()
		val content = "HELLO WORLD!"
		temp["korio.temp"].writeString(content)
		temp["korio.temp2"].writeFile(temp["korio.temp"])
		Assert.assertEquals(content, temp["korio.temp2"].readString())
		Unit
	}
}