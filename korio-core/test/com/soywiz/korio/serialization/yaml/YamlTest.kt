package com.soywiz.korio.serialization.yaml

import org.junit.Test
import kotlin.test.assertEquals

// http://nodeca.github.io/js-yaml/
class YamlTest {
	@Test
	fun basic() {
		assertEquals("str", Yaml.read("str"))
		assertEquals(10, Yaml.read("10"))
	}

	@Test
	fun array() {
		assertEquals(listOf(1, 2, 3), Yaml.read("[1,2,3]"))
	}

	@Test
	fun name() {
		assertEquals(
			listOf(1, 2, 3),
			Yaml.read("""
			- 1
			- 2
			- 3
			""".trimIndent())
		)
	}

	@Test
	fun name2() {
		assertEquals(
			mapOf("hr" to 65, "avg" to 0.278, "rbi" to 147),
			Yaml.read("""
				hr:  65    # Home runs
				avg: 0.278 # Batting average
				rbi: 147   # Runs Batted In
			""".trimIndent())
		)
	}

	@Test
	fun name3() {
		assertEquals(
			listOf(listOf(listOf(1))),
			Yaml.read("- - - 1")
		)
	}

	@Test
	fun name4() {
		assertEquals(
			listOf(mapOf("a" to 1), mapOf("a" to 2)),
			Yaml.read("""
				|-
				|	a: 1
				|-
				|	a: 2
			""".trimMargin())
		)
	}

	@Test
	fun name5() {
		assertEquals(
			listOf(
				mapOf(
					"name" to "Mark McGwire",
					"hr" to 65,
					"avg" to 0.278
				),
				mapOf(
					"name" to "Sammy Sosa",
					"hr" to 63,
					"avg" to 0.288
				)
			),
			Yaml.read("""
				|-
				|  name: Mark McGwire
				|  hr:   65
				|  avg:  0.278
				|-
				| name: Sammy Sosa
				| hr:   63
				| avg:  0.288
			""".trimMargin())
		)
	}

	@Test
	fun name6() {
		assertEquals(
			mapOf(
				"hr" to listOf("Mark McGwire", "Sammy Sosa"),
				"rbi" to listOf("Sammy Sosa", "Ken Griffey")
			),
			Yaml.read("""
				|hr: # 1998 hr ranking
				|  - Mark McGwire
				|  - Sammy Sosa
				|rbi:
				|  # 1998 rbi ranking
				|  - Sammy Sosa
				|  - Ken Griffey
			""".trimMargin())
		)
	}

	@Test
	fun name7() {
		assertEquals(
			mapOf(
				"null" to null,
				"booleans" to listOf(true, false),
				"string" to "012345"
			),
			Yaml.read("""
				|null:
				|booleans: [ true, false ]
				|string: '012345'

			""".trimMargin())
		)
	}

	enum class MyEnum { DEMO, HELLO, WORLD }
	data class ClassWithEnum(val size: Int = 70, val a: MyEnum = MyEnum.HELLO)

	@Test
	fun decodeToType() {
		assertEquals(
			ClassWithEnum(a = MyEnum.WORLD),
			Yaml.decodeToType<ClassWithEnum>("""
				|a: WORLD
			""".trimMargin())
		)
	}

	//@Test
	//fun name8() {
	//	assertEquals(
	//		null,
	//		Yaml.read("[a:1,b:2]")
	//	)
	//}
}