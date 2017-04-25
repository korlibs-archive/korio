package com.soywiz.korio.util

import com.soywiz.korio.error.invalidOp
import java.lang.Boolean
import java.lang.Byte
import java.lang.Double
import java.lang.Float
import java.lang.Long
import java.lang.Short
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

// @TODO: This should use ASM library to create a class per class to be as fast as possible
class ClassFactory<T> private constructor(iclazz: Class<out T>, internal: kotlin.Boolean) {
	val clazz = when {
		List::class.java.isAssignableFrom(iclazz) -> ArrayList::class.java
		Map::class.java.isAssignableFrom(iclazz) -> HashMap::class.java
		else -> iclazz
	}

	init {
		//println("$iclazz -> $clazz")
	}

	companion object {
		val cache = hashMapOf<Class<*>, ClassFactory<*>>()
		@Suppress("UNCHECKED_CAST")
		operator fun <T> get(clazz: Class<out T>): ClassFactory<T> = cache.getOrPut(clazz) { ClassFactory(clazz, true) } as ClassFactory<T>

		operator fun <T> invoke(clazz: Class<out T>): ClassFactory<T> = ClassFactory[clazz]

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
			if (clazz.isAssignableFrom(Set::class.java)) return HashSet<Any>()
			if (clazz.isAssignableFrom(List::class.java)) return ArrayList<Any>()
			if (clazz.isAssignableFrom(Map::class.java)) return HashMap<Any, Any>()
			if (clazz.isEnum) return clazz.enumConstants.first()
			return ClassFactory[clazz].createDummy()
		}
	}

	val constructor = clazz.declaredConstructors.sortedBy { it.parameterTypes.size }.firstOrNull() ?: invalidOp("Can't find constructor for $clazz")
	val dummyArgs = createDummyArgs(constructor)
	val fields = clazz.declaredFields
		.filter { !Modifier.isTransient(it.modifiers) && !Modifier.isStatic(it.modifiers) }

	init {
		constructor.isAccessible = true
		for (field in fields) field.isAccessible = true
	}

	fun create(values: Any?): T {
		when (values) {
			is Map<*, *> -> {
				val instance = createDummy()
				for (field in fields) {
					if (values.containsKey(field.name)) {
						field.isAccessible = true
						field.set(instance, Dynamic.dynamicCast(values[field.name], field.type, field.genericType))
					}
				}
				return instance
			}
			else -> {
				return Dynamic.dynamicCast(values, clazz as Class<*>) as T
			}
		}
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
