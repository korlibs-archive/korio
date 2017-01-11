package com.soywiz.korio.serialization.binary

import com.soywiz.korio.stream.*
import org.junit.Assert
import org.junit.Test

class StructTest {
	@Size(8)
	@LE
	data class Demo(
		@Offset(0) val a: Int,
		@Offset(4) @BE val b: Int
	) : Struct

	@Size(12)
	data class Composed(
		@Offset(0) val a: Int,
		@Offset(4) val b: Demo
	) : Struct

	class StructWithArray(
		@Offset(0) @Count(10) val a: IntArray
	) : Struct

	class StructWithString(
		@Offset(0) @Count(20) @Encoding("UTF-8") @JvmField val a: String
	) : Struct

	@Test
	fun name() {
		val mem = MemorySyncStream()
		mem.write32_le(7)
		mem.write32_be(77)
		mem.position = 0
		val demo = mem.readStruct<Demo>()
		Assert.assertEquals(7, demo.a)
		Assert.assertEquals(77, demo.b)
		mem.writeStruct(demo)
		Assert.assertEquals(16, mem.length)
		mem.position = 8
		Assert.assertEquals(7, mem.readS32_le())
		Assert.assertEquals(77, mem.readS32_be())
	}

	@Test
	fun name2() {
		val mem = MemorySyncStream()
		mem.writeStruct(Composed(1, Demo(2, 3)))
		Assert.assertEquals(12, mem.position)
		mem.position = 0
		Assert.assertEquals(1, mem.readS32_le())
		Assert.assertEquals(2, mem.readS32_le())
		Assert.assertEquals(3, mem.readS32_be())
	}

	@Test
	fun name3() {
		val mem = MemorySyncStream()
		Assert.assertEquals(4 * 10, StructWithArray::class.java.getStructSize())
		mem.writeStruct(StructWithArray(intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
		Assert.assertEquals(4 * 10, mem.position)
		mem.position = 0
		val info = mem.readStruct<StructWithArray>()
		Assert.assertEquals("[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]", info.a.toList().toString())
	}

	@Test
	fun name4() {
		val mem = MemorySyncStream()
		mem.writeStruct(StructWithString("hello"))
		Assert.assertEquals(20, mem.position)
		mem.position = 0
		val info = mem.readStruct<StructWithString>()
		Assert.assertEquals("hello", info.a)
	}
}