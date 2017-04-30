package com.soywiz.korio.serialization.json

import org.junit.Assert
import org.junit.Test

class JsonTest {
	@Test
	fun decode1() {
		Assert.assertEquals(mapOf("a" to 1), Json.decode("""{"a":1}"""))
		Assert.assertEquals(-1e7, Json.decode("""-1e7"""))
	}

	@Test
	fun decode2() {
		Assert.assertEquals(
			listOf("a", 1, -1, 0.125, 0, 11, true, false, null, listOf<Any?>(), mapOf<String, Any?>()),
			Json.decode("""["a", 1, -1, 0.125, 0, 11, true, false, null, [], {}]""")
		)
	}

	@Test
	fun decode3() {
		Assert.assertEquals("\"", Json.decode(""" "\"" """))
		Assert.assertEquals(listOf(1, 2), Json.decode(""" [ 1 , 2 ]"""))
	}

	@Test
	fun encode1() {
		Assert.assertEquals("1", Json.encode(1))
		Assert.assertEquals("null", Json.encode(null))
		Assert.assertEquals("true", Json.encode(true))
		Assert.assertEquals("false", Json.encode(false))
		Assert.assertEquals("{}", Json.encode(mapOf<String, Any?>()))
		Assert.assertEquals("[]", Json.encode(listOf<Any?>()))
		Assert.assertEquals("\"a\"", Json.encode("a"))
	}

	@Test
	fun encode2() {
		Assert.assertEquals("[1,2,3]", Json.encode(listOf(1, 2, 3)))
		Assert.assertEquals("""{"a":1,"b":2}""", Json.encode(hashMapOf("a" to 1, "b" to 2)))
	}

	data class Demo(val a: Int, val b: String)

	data class DemoList(val demos: ArrayList<Demo>)

	data class DemoSet(val demos: Set<Demo>)

	@Test
	fun encodeTyped() {
		Assert.assertEquals("""{"a":1,"b":"test"}""", Json.encode(Demo(1, "test")))
	}

	@Test
	fun decodeToType1() {
		Assert.assertEquals(Demo(1, "hello"), Json.decodeToType<Demo>("""{"a": 1, "b": "hello"}"""))
	}

	@Test
	fun decodeUnicode() {
		Assert.assertEquals("aeb", Json.decode(""" "a\u0065b" """))
	}

	@Test
	fun decodeTypedList() {
		val result = Json.decodeToType<DemoList>("""{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""")
		val demo = result.demos.first()
		Assert.assertEquals(1, demo.a)
	}

	@Test
	fun decodeTypedSet() {
		val result = Json.decodeToType<DemoSet>("""{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""")
		val demo = result.demos.first()
		Assert.assertEquals(1, demo.a)
	}

	@Test
	fun decodeToPrim() {
		//val resultStr = Json.encode(mapOf("items" to listOf(1, 2, 3, 4, 5)))
		Assert.assertEquals(listOf(1, 2, 3, 4, 5), Json.decodeToType<List<Int>>("""[1, 2, 3, 4, 5]"""))
		Assert.assertEquals(1, Json.decodeToType<Int>("1"))
		Assert.assertEquals(true, Json.decodeToType<Boolean>("true"))
		Assert.assertEquals("a", Json.decodeToType<String>("\"a\""))
		Assert.assertEquals('a', Json.decodeToType<Char>("\"a\""))
	}

	@Test
	fun decodeToPrimChar() {
		Assert.assertEquals('a', Json.decodeToType<Char>("\"a\""))
	}

	@Test
	fun decodeWithStaticMembers() {
		Assert.assertEquals("""{"a":10}""", Json.encode(Demo2()))
	}

	enum class MyEnum { DEMO, HELLO, WORLD }
	data class ClassWithEnum(val a: MyEnum = MyEnum.HELLO)

	@Test
	fun testEncodeEnum() {
		Assert.assertEquals("""{"a":"HELLO"}""", Json.encode(ClassWithEnum()))
	}

	@Test
	fun testDecodeEnum() {
		Assert.assertEquals(ClassWithEnum(MyEnum.WORLD), Json.decodeToType<ClassWithEnum>("""{"a":"WORLD"}"""))
	}

	class Demo2 {
		var a: Int = 10

		companion object {
			@JvmStatic var b: String = "test"
		}
	}
}