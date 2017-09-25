package com.soywiz.korio.serialization

import com.soywiz.korio.error.invalidArg
import com.soywiz.korio.lang.DynamicContext
import com.soywiz.korio.lang.KClass
import com.soywiz.korio.lang.classOf

/**
 * Register classes and how to type and untype them.
 *
 * To Type means to generate typed domain-specific objects
 * While to untype meanas to convert those objects into supported generic primitives:
 * Bools, Numbers, Strings, Lists and Maps (json supported)
 */
class ObjectMapper {
	val _typers = HashMap<KClass<*>, TypeContext.(Any?) -> Any?>()
	val _untypers = HashMap<KClass<*>, UntypeContext.(Any?) -> Any?>()

	@Suppress("NOTHING_TO_INLINE")
	class TypeContext(val map: ObjectMapper) : DynamicContext {
		inline fun <reified T> Any?.gen(): T = map.toTyped(this, T::class)
		inline fun <reified T> Any?.genList(): ArrayList<T> {
			return ArrayList(this.toDynamicList().map { it.gen<T>() }.toList())
		}

		inline fun <reified T> Any?.genSet(): HashSet<T> {
			return HashSet(this.toDynamicList().map { it.gen<T>() }.toSet())
		}

		inline fun <reified K, reified V> Any?.genMap(): HashMap<K, V> {
			return HashMap(this.toDynamicMap().map { it.key.gen<K>() to it.value.gen<V>() }.toMap())
		}
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

	fun <T> registerType(clazz: KClass<T>, generate: TypeContext.(Any?) -> T) = this.apply {
		_typers[clazz] = generate
	}

	fun <T> toTyped(obj: Any?, clazz: KClass<T>): T {
		val generator = _typers[clazz] ?: invalidArg("Unregistered $clazz")
		return generator(typeCtx, obj) as T
	}

	init {
		registerType(Boolean::class) { it.toBool() }
		registerType(Byte::class) { it.toByte() }
		registerType(Char::class) { it.toChar() }
		registerType(Short::class) { it.toShort() }
		registerType(Int::class) { it.toInt() }
		registerType(Long::class) { it.toLong() }
		registerType(Float::class) { it.toFloat() }
		registerType(Double::class) { it.toDouble() }
		//registerType(Number::class) { it.toNumber() } // @TODO: This produces an undefined error in kotlin-js
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
			val unt = _untypers[clazz]
			if (unt == null) {
				println("Untypers: " + _untypers.size)
				for (u in _untypers) {
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
		_untypers[clazz] = untyper as UntypeContext.(Any?) -> Any?
	}

	inline fun <reified T> registerUntype(noinline untyper: UntypeContext.(T) -> Any?) = registerUntype(classOf<T>(), untyper)

	inline fun <reified T : Enum<T>> registerUntypeEnum() = registerUntype<T> { it.name }
	//inline fun <reified T> registerUntypeObj(vararg props: KPro<T>) = registerUntype(classOf<T>(), untyper)

	inline fun <T> scoped(callback: () -> T): T {
		val oldTypers = _typers.toMap()
		val oldUntypers = _untypers.toMap()
		try {
			return callback()
		} finally {
			_typers.clear()
			_typers.putAll(oldTypers)
			_untypers.clear()
			_untypers.putAll(oldUntypers)
		}
	}
}

val Mapper = ObjectMapper()