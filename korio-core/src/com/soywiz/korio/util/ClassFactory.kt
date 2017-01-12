package com.soywiz.korio.util

import java.lang.Boolean
import java.lang.Byte
import java.lang.Double
import java.lang.Float
import java.lang.Long
import java.lang.Short
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

// @TODO: This should use ASM library to create a class per class to be as fast as possible
class ClassFactory<T>(val clazz: Class<T>) {
	companion object {
		val cache = hashMapOf<Class<*>, ClassFactory<*>>()
		@Suppress("UNCHECKED_CAST")
		operator fun <T> get(clazz: Class<T>): ClassFactory<T> = cache.getOrPut(clazz) { ClassFactory(clazz) } as ClassFactory<T>

		fun createDummyUnchecked(clazz: Class<*>): Any {
			when (clazz) {
				Boolean.TYPE -> return false
				Byte.TYPE -> return 0.toByte()
				Short.TYPE -> return 0.toShort()
				Character.TYPE -> return 0.toChar()
				Integer.TYPE -> return 0
				Long.TYPE -> return 0L
				Float.TYPE -> return 0f
				Double.TYPE -> return 0.0
			}
			if (clazz.isArray) return java.lang.reflect.Array.newInstance(clazz.componentType, 0)
			if (clazz.isAssignableFrom(List::class.java)) return ArrayList<Any>()
			if (clazz.isAssignableFrom(Map::class.java)) return HashMap<Any, Any>()
			return ClassFactory[clazz].createDummy()
		}
	}

	val constructor = clazz.declaredConstructors.first()
	val dummyArgs = createDummyArgs(constructor)
	val fields = clazz.declaredFields
		.filter { !Modifier.isTransient(it.modifiers) }

	init {
		for (field in fields) {
			field.isAccessible = true
		}
	}

	fun create(values: Map<String, Any?>): T {
		val instance = createDummy()
		for (field in fields) {
			if (values.containsKey(field.name)) {
				field.set(instance, values[field.name])
			}
		}
		return instance
	}

	fun toMap(instance: T): Map<String, Any?> {
		return fields.map { it.name to it.get(instance) }.toMap()
	}

	@Suppress("UNCHECKED_CAST")
	fun createDummy(): T = constructor.newInstance(*dummyArgs) as T

	fun createDummyArgs(constructor: Constructor<*>): Array<Any> {
		return constructor.parameterTypes.map { createDummyUnchecked(it) }.toTypedArray()
	}
}
