package com.soywiz.korio.serialization.yaml

import com.soywiz.korio.dynamic.mapper.*
import com.soywiz.korio.dynamic.serialization.*
import kotlin.test.*

// http://nodeca.github.io/js-yaml/
class YamlTest {
	val mapper = ObjectMapper()

	@kotlin.test.Test
	fun basic() {
		assertEquals("str", Yaml.read("str"))
		assertEquals(10, Yaml.read("10"))
	}

	@kotlin.test.Test
	fun array() {
		assertEquals(listOf(1, 2, 3), Yaml.read("[1,2,3]"))
	}

	@kotlin.test.Test
	fun name() {
		assertEquals(
			listOf(1, 2, 3),
			Yaml.read(
				"""
			- 1
			- 2
			- 3
			""".trimIndent()
			)
		)
	}

	@kotlin.test.Test
	fun name2() {
		assertEquals(
			linkedMapOf("hr" to 65, "avg" to 0.278, "rbi" to 147),
			Yaml.read(
				"""
				hr:  65    # Home runs
				avg: 0.278 # Batting average
				rbi: 147   # Runs Batted In
			""".trimIndent()
			)
		)
	}

	@kotlin.test.Test
	fun name3() {
		assertEquals(
			listOf(listOf(listOf(1))),
			Yaml.read("- - - 1")
		)
	}

	@kotlin.test.Test
	fun name4() {
		assertEquals(
			listOf(linkedMapOf("a" to 1), linkedMapOf("a" to 2)),
			Yaml.read(
				"""
				|-
				|	a: 1
				|-
				|	a: 2
			""".trimMargin()
			)
		)
	}

	@kotlin.test.Test
	fun name5() {
		assertEquals(
			listOf(
				linkedMapOf(
					"name" to "Mark McGwire",
					"hr" to 65,
					"avg" to 0.278
				),
				linkedMapOf(
					"name" to "Sammy Sosa",
					"hr" to 63,
					"avg" to 0.288
				)
			),
			Yaml.read(
				"""
				|-
				|  name: Mark McGwire
				|  hr:   65
				|  avg:  0.278
				|-
				| name: Sammy Sosa
				| hr:   63
				| avg:  0.288
			""".trimMargin()
			)
		)
	}

	@kotlin.test.Test
	fun name6() {
		assertEquals(
			linkedMapOf(
				"hr" to listOf("Mark McGwire", "Sammy Sosa"),
				"rbi" to listOf("Sammy Sosa", "Ken Griffey")
			),
			Yaml.read(
				"""
				|hr: # 1998 hr ranking
				|  - Mark McGwire
				|  - Sammy Sosa
				|rbi:
				|  # 1998 rbi ranking
				|  - Sammy Sosa
				|  - Ken Griffey
			""".trimMargin()
			)
		)
	}

	@kotlin.test.Test
	fun name7() {
		assertEquals(
			linkedMapOf(
				"null" to null,
				"booleans" to listOf(true, false),
				"string" to "012345"
			),
			Yaml.read(
				"""
				|null:
				|booleans: [ true, false ]
				|string: '012345'

			""".trimMargin()
			)
		)
	}

	enum class MyEnum { DEMO, HELLO, WORLD }
	data class ClassWithEnum(val size: Int = 70, val a: MyEnum = MyEnum.HELLO)

	@kotlin.test.Test
	fun decodeToType() {
		mapper.registerEnum(MyEnum.values())
		mapper.registerType { ClassWithEnum(it["size"]?.gen() ?: 70, it["a"]?.gen() ?: MyEnum.HELLO) }

		assertEquals(
			ClassWithEnum(a = MyEnum.WORLD),
			Yaml.decodeToType<ClassWithEnum>(
				"""
				|a: WORLD
			""".trimMargin(), mapper
			)
		)
	}

    @Test
    fun testChunk() {
        val yamlStr = """
        layout: post
        layout2: null
        demo: false
        permalink: /lorem-ipsum/
        title: "Lorem Ipsum"
        feature_image: "/images/2019/lorem_ipsum.jpg"
        tags: [lorem_ipsum]
        date: 2019-10-07 00:00:00 
        """.trimIndent()
        //println(Yaml.tokenize(yamlStr))
        assertEquals(
            mapOf(
                "layout" to "post",
                "layout2" to null,
                "demo" to false,
                "permalink" to "/lorem-ipsum/",
                "title" to "Lorem Ipsum",
                "feature_image" to "/images/2019/lorem_ipsum.jpg",
                "tags" to listOf("lorem_ipsum"),
                "date" to "2019-10-07 00:00:00"
            ),
            Yaml.decode(yamlStr)
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
