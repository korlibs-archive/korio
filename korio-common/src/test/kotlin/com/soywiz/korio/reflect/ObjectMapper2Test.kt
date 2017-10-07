package com.soywiz.korio.reflect

import org.junit.Test
import kotlin.test.assertEquals

class ObjectMapper2Test {
	data class Person(val name: String, var surname: String)

	val om = ObjectMapper2()
		.register(
			ClassReflect(
				Person::class,
				listOf(Person::name, Person::surname),
				listOf(String::class, String::class)
			) { Person(get(0), get(1)) }
		)

	@Test
	fun testGetter() {
		val soywiz = Person("Soywiz", "Zard Zard")

		assertEquals("Soywiz", om.get(soywiz, "name"))
		assertEquals("Zard Zard", om.get(soywiz, "surname"))

		om.set(soywiz, "surname", "NoSurname")

		assertEquals("NoSurname", om.get(soywiz, "surname"))

		val carlos = om.create(Person::class, mapOf("name" to "Carlos", "surname" to "Ballesteros"))
		val carlos2 = om.create(Person::class, listOf("Carlos", "Ballesteros"))
		val types2 = om.getTypes(Person::class)

		assertEquals("Person(name=Carlos, surname=Ballesteros)", carlos.toString())
		assertEquals("Person(name=Carlos, surname=Ballesteros)", carlos2.toString())
		assertEquals(listOf(String::class, String::class), types2)
	}
}