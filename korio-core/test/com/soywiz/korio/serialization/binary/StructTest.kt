package com.soywiz.korio.serialization.binary

/*
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

	data class NoSizeNoOffset(
		@Order(0) val magic: Int,
		@Order(1) val ver1: Byte,
		@Order(2) val ver2: Byte
	) : Struct

	@Suppress("ArrayInDataClass")
	data class NoSizeNoOffsetArray(
		@Order(0) val magic: Int,
		@Order(1) @Count(2) val items: Array<NoSizeNoOffset>,
		@Order(2) val v2: Int
	) : Struct

	// Not supported yet!
	//class DynamicLength(
	//	@Order(0) val magic: Int,
	//	@Order(1) val len: Int,
	//	@Order(2) @DynamicCount("len") @Encoding("UTF-8") val str: String
	//) : Struct

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

	@Test
	fun name4b() {
		val mem = MemorySyncStream()
		mem.writeStringz("hello", 20)
		mem.position--
		mem.write8(3)
		mem.position = 0
		val info = mem.readStruct<StructWithString>()
		Assert.assertEquals("hello", info.a)
	}

	@Test
	fun name5() {
		val mem = MemorySyncStream()
		mem.writeStruct(NoSizeNoOffset(1, 2, 3))
		Assert.assertEquals(6, mem.position)
		mem.position = 0
		val info = mem.readStruct<NoSizeNoOffset>()
		Assert.assertEquals("NoSizeNoOffset(magic=1, ver1=2, ver2=3)", info.toString())
	}

	@Test
	fun name6() {
		val mem = MemorySyncStream()
		mem.writeStruct(NoSizeNoOffsetArray(1, arrayOf(NoSizeNoOffset(2, 3, 4), NoSizeNoOffset(5, 6, 7)), 8))
		Assert.assertEquals(4 + (6 * 2) + 4, mem.position)
		mem.position = 0
		val info = mem.readStruct<NoSizeNoOffsetArray>()
		Assert.assertEquals("NoSizeNoOffsetArray(magic=1, items=[NoSizeNoOffset(magic=2, ver1=3, ver2=4), NoSizeNoOffset(magic=5, ver1=6, ver2=7)], v2=8)", info.toString())
	}
}
*/
