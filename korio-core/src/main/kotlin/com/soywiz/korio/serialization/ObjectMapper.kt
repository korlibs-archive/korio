package com.soywiz.korio.serialization

import com.soywiz.korio.error.invalidArg
import com.soywiz.korio.lang.DynamicContext
import com.soywiz.korio.lang.KClass
import com.soywiz.korio.lang.classOf
import com.soywiz.korio.util.nonNullMap

/**
 * Register classes and how to type and untype them.
 *
 * To Type means to generate typed domain-specific objects
 * While to untype meanas to convert those objects into supported generic primitives:
 * Bools, Numbers, Strings, Lists and Maps (json supported)
 */
class ObjectMapper {
	class TypeContext(val map: ObjectMapper) : DynamicContext {
		inline fun <reified T> Any?.gen(): T = map.toTyped(this, T::class)
	}

	@Suppress("NOTHING_TO_INLINE")
	class UntypeContext(val map: ObjectMapper) {
		inline fun <reified T> T.gen(): Any? = map.toUntyped(this)
		inline fun Boolean.gen(): Any? = this
		inline fun String.gen(): Any? = this
		inline fun Number.gen(): Any? = this
		inline fun <reified T> Iterable<T>.gen(): List<Any?> = this.map { it.gen() }
		inline fun <reified K, reified V> Map<K, V>.gen(): Map<Any?, Any?> = this.map { it.key.gen() to it.value.gen() }.toMap()
	}

	private val typeCtx = TypeContext(this)
	private val untypeCtx = UntypeContext(this)

	private val typers = HashMap<KClass<*>, TypeContext.(Any?) -> Any?>()
	private val untypers = HashMap<KClass<*>, UntypeContext.(Any?) -> Any?>()

	fun <T> registerType(clazz: KClass<T>, generate: TypeContext.(Any?) -> T) = this.apply {
		typers[clazz] = generate
	}

	fun <T> toTyped(obj: Any?, clazz: KClass<T>): T {
		val generator = typers[clazz] ?: invalidArg("Unregistered $clazz")
		return generator(typeCtx, obj) as T
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

	inline fun <reified T> toUntyped(obj: T): Any? = toUntyped(classOf<T>(), obj)

	fun toUntyped(clazz: KClass<Any?>, obj: Any?): Any? = when (obj) {
		null -> obj
		is Boolean -> obj
		is Number -> obj
		is String -> obj
		is Iterable<*> -> ArrayList(obj.map { toUntyped(it) })
		is Map<*, *> -> HashMap(obj.map { toUntyped(it.key) to toUntyped(it.value) }.toMap())
		else -> {
			val unt = untypers[clazz]
			if (unt == null) {
				println("Untypers: " + untypers.size)
				for (u in untypers) {
					println(" - " + u.key)
				}

				invalidArg("Don't know how to untype $clazz")
			}
			unt.invoke(untypeCtx, obj)
		}
	}

	fun <T : Enum<T>> registerEnum(clazz: KClass<T>, values: Array<T>) {
		val nameToString = values.map { it.name to it }.toMap()
		registerType(clazz) { nameToString[it.toString()] }
	}

	inline fun <reified T> registerType(noinline generate: TypeContext.(Any?) -> T) = registerType(classOf<T>(), generate)
	inline fun <reified T : Enum<T>> registerEnum(values: Array<T>) = registerEnum(classOf<T>(), values)

	fun <T> registerUntype(clazz: KClass<T>, untyper: UntypeContext.(T) -> Any?) {
		untypers[clazz] = untyper as UntypeContext.(Any?) -> Any?
	}

	inline fun <reified T> registerUntype(noinline untyper: UntypeContext.(T) -> Any?) = registerUntype(classOf<T>(), untyper)

	inline fun <reified T : Enum<T>> registerUntypeEnum() = registerUntype<T> { it.name }
	//inline fun <reified T> registerUntypeObj(vararg props: KPro<T>) = registerUntype(classOf<T>(), untyper)
}