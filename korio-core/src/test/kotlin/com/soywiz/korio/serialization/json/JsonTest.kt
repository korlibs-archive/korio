package com.soywiz.korio.serialization.json

import com.soywiz.korio.serialization.ObjectMapper
import org.junit.Test
import kotlin.test.assertEquals

class JsonTest {
	enum class MyEnum { DEMO, HELLO, WORLD }
	data class ClassWithEnum(val a: MyEnum = MyEnum.HELLO)

	class Demo2 {
		var a: Int = 10

		companion object {
			//@JvmField
			var b: String = "test"
		}
	}

	data class Demo(val a: Int, val b: String)

	data class DemoList(val demos: ArrayList<Demo>)

	data class DemoSet(val demos: Set<Demo>)

	val mapper = ObjectMapper()

	init {
		// GENERATED. THIS CODE SHOULD BE GENERATED.
		mapper.registerEnum(MyEnum.values())
		mapper.registerType { Demo(it["a"].gen(), it["b"].gen()) }
		mapper.registerType { Demo2().apply { a = it["a"].gen() } }
		mapper.registerType { DemoList(it["demos"].toDynamicList().map { it.gen<Demo>() }.toArrayList()) }
		mapper.registerType { DemoSet(it["demos"].toDynamicList().map { it.gen<Demo>() }.toSet()) }
		mapper.registerType { ClassWithEnum(it["a"]?.gen() ?: MyEnum.HELLO) }

		mapper.registerUntypeEnum<MyEnum>()
		mapper.registerUntype<ClassWithEnum> { mapOf("a" to it.a.gen()) }
		mapper.registerUntype<Demo2> { mapOf("a" to it.a.gen()) }
		mapper.registerUntype<Demo> { mapOf("a" to it.a.gen(), "b" to it.b.gen()) }
		mapper.registerUntype<DemoList> { mapOf("demos" to it.demos.gen()) }
		mapper.registerUntype<DemoSet> { mapOf("demos" to it.demos.gen()) }
	}

	@Test
	fun decode1() {
		assertEquals(mapOf("a" to 1), Json.decode("""{"a":1}"""))
		assertEquals(-1e7, Json.decode("""-1e7"""))
	}

	@Test
	fun decode2() {
		assertEquals(
			listOf("a", 1, -1, 0.125, 0, 11, true, false, null, listOf<Any?>(), mapOf<String, Any?>()),
			Json.decode("""["a", 1, -1, 0.125, 0, 11, true, false, null, [], {}]""")
		)
	}

	@Test
	fun decode3() {
		assertEquals("\"", Json.decode(""" "\"" """))
		assertEquals(listOf(1, 2), Json.decode(""" [ 1 , 2 ]"""))
	}

	@Test
	fun encode1() {
		assertEquals("1", Json.encode(1, mapper))
		assertEquals("null", Json.encode<Any?>(null, mapper))
		assertEquals("true", Json.encode(true, mapper))
		assertEquals("false", Json.encode(false, mapper))
		assertEquals("{}", Json.encode(mapOf<String, Any?>(), mapper))
		assertEquals("[]", Json.encode(listOf<Any?>(), mapper))
		assertEquals("\"a\"", Json.encode("a", mapper))
	}

	@Test
	fun encode2() {
		assertEquals("[1,2,3]", Json.encode(listOf(1, 2, 3), mapper))
		assertEquals("""{"a":1,"b":2}""", Json.encode(hashMapOf("a" to 1, "b" to 2), mapper))
	}

	@Test
	fun encodeTyped() {
		assertEquals("""{"a":1,"b":"test"}""", Json.encode(Demo(1, "test"), mapper))
	}

	@Test
	fun decodeToType1() {
		assertEquals(Demo(1, "hello"), Json.decodeToType<Demo>("""{"a": 1, "b": "hello"}""", mapper))
	}

	@Test
	fun decodeUnicode() {
		assertEquals("aeb", Json.decode(""" "a\u0065b" """))
	}

	fun <T> Iterable<T>.toArrayList(): ArrayList<T> {
		return ArrayList<T>().apply { addAll(this@toArrayList) }
	}

	@Test
	fun decodeTypedList() {
		val result = Json.decodeToType<DemoList>("""{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""", mapper)
		val demo = result.demos.first()
		assertEquals(1, demo.a)
	}

	@Test
	fun decodeTypedSet() {
		val result = Json.decodeToType<DemoSet>("""{ "demos" : [{"a":1,"b":"A"}, {"a":2,"b":"B"}] }""", mapper)
		val demo = result.demos.first()
		assertEquals(1, demo.a)
	}

	@Test
	fun decodeToPrim() {
		//val resultStr = Json.encode(mapOf("items" to listOf(1, 2, 3, 4, 5)))
		assertEquals(listOf(1, 2, 3, 4, 5), Json.decodeToType<List<Int>>("""[1, 2, 3, 4, 5]""", mapper))
		assertEquals(1, Json.decodeToType<Int>("1", mapper))
		assertEquals(true, Json.decodeToType<Boolean>("true", mapper))
		assertEquals("a", Json.decodeToType<String>("\"a\"", mapper))
		assertEquals('a', Json.decodeToType<Char>("\"a\"", mapper))
	}

	@Test
	fun decodeToPrimChar() {
		assertEquals('a', Json.decodeToType<Char>("\"a\"", mapper))
	}

	@Test
	fun encodeWithStaticMembers() {
		assertEquals("""{"a":10}""", Json.encode(Demo2(), mapper))
	}

	@Test
	fun testEncodeEnum() {
		assertEquals("""{"a":"HELLO"}""", Json.encode(ClassWithEnum(), mapper))
	}

	@Test
	fun testDecodeEnum() {
		assertEquals(ClassWithEnum(MyEnum.WORLD), Json.decodeToType<ClassWithEnum>("""{"a":"WORLD"}""", mapper))
	}

	@Test
	fun testDecodeMap() {
		data class V(val a: Int, val b: Int)
		data class Demo(val v: Map<String, V>)

		mapper.registerType { V(it["a"].gen(), it["b"].gen()) }
		mapper.registerType { Demo(it["v"].toDynamicMap().map { it.key.toString() to it.value.gen<V>() }.toMap()) }

		assertEquals(Demo(mapOf("z" to V(1, 2))), Json.decodeToType<Demo>("""{"v":{"z":{"a":1,"b":2}}}""", mapper))
	}

	@Test
	fun testEncodeMap() {
		data class V(val a: Int, val b: Int)
		data class Demo(val v: Map<String, V>)

		mapper.registerUntype<V> { mapOf("a" to it.a.gen(), "b" to it.b.gen()) }
		mapper.registerUntype<Demo> { mapOf("v" to it.v.gen()) }

		assertEquals("""{"v":{"z1":{"a":1,"b":2},"z2":{"a":1,"b":2}}}""", Json.encode(Demo(mapOf("z1" to V(1, 2), "z2" to V(1, 2))), mapper))
	}
}