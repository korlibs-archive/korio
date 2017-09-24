package com.soywiz.korio.vfs

import com.soywiz.korio.async.syncTest
import org.junit.Test
import kotlin.test.assertEquals

class ReadSpecialTest {
	class CharArray2(val width: Int, val height: Int, val data: CharArray) {
		fun index(x: Int, y: Int): Int = y * width + x
		operator fun get(x: Int, y: Int): Char = data[index(x, y)]
		operator fun set(x: Int, y: Int, v: Char): Unit = run { data[index(x, y)] = v }
	}

	suspend fun VfsFile.readCharArray2(): CharArray2 {
		val text = this.readString()
		val side = Math.sqrt(text.length.toDouble()).toInt()
		return CharArray2(side, side, text.toCharArray())
	}

	@Test
	fun testReadSpecial2() = syncTest {
		val temp = MemoryVfs()
		val f2 = temp["korio.chararray2"]
		f2.writeString("123456789")
		val c2 = f2.readCharArray2()
		assertEquals('1', c2[0, 0])
		assertEquals('5', c2[1, 1])
		assertEquals('9', c2[2, 2])
	}
}