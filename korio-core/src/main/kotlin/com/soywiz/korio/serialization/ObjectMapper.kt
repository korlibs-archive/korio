package com.soywiz.korio.serialization

import com.soywiz.korio.error.invalidArg
import com.soywiz.korio.lang.DynamicContext
import com.soywiz.korio.lang.KClass
import com.soywiz.korio.lang.classOf

class ObjectMapper {
	class GenContext(val map: ObjectMapper) : DynamicContext {
		inline fun <reified T> Any?.gen(): T = map.toTyped(this, T::class)
	}

	private val ctx = GenContext(this)
	private val generators = HashMap<KClass<*>, GenContext.(Any?) -> Any?>()

	fun <T> registerType(clazz: KClass<T>, generate: GenContext.(Any?) -> T) = this.apply {
		generators[clazz] = generate
	}

	fun <T> toTyped(obj: Any?, clazz: KClass<T>): T {
		val generator = generators[clazz] ?: invalidArg("Unregistered $clazz")
		return generator(ctx, obj) as T
	}

	init {
		registerType(Boolean::class) { it.toBool() }
		registerType(Byte::class) { it.toByte() }
		registerType(Char::class) { it.toChar() }
		registerType(Short::class) { it.toShort() }
		registerType(Int::class) { it.toInt() }
		registerType(Long::class) { it.toLong() }
		registerType(Double::class) { it.toDouble() }
		registerType(Number::class) { it.toNumber() }
		registerType(Set::class) { it.toDynamicList().toSet() }
		registerType(List::class) { it.toDynamicList() }
		registerType(MutableList::class) { it.toDynamicList().toMutableList() }
		registerType(String::class) { it?.toString() ?: "null" }
	}

	fun toUntyped(obj: Any?): Any? {
		return when (obj) {
			null -> obj
			is Number -> obj
			is Iterable<*> -> ArrayList(obj.map { toUntyped(it) })
			is Map<*, *> -> HashMap(obj.map { toUntyped(it.key) to toUntyped(it.value) }.toMap())
			else -> TODO()
		}
	}

	fun <T : Enum<T>> registerEnum(clazz: KClass<T>, values: Array<T>) {
		val nameToString = values.map { it.name to it }.toMap()
		registerType(clazz) { nameToString[it.toString()] }
	}

	inline fun <reified T> registerType(noinline generate: GenContext.(Any?) -> T) = registerType(classOf<T>(), generate)
	inline fun <reified T : Enum<T>> registerEnum(values: Array<T>) = registerEnum(classOf<T>(), values)
}