package com.soywiz.korio.util

import com.soywiz.korio.async.invokeSuspend
import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.error.noImpl
import java.lang.Byte
import java.lang.Enum
import java.lang.Float
import java.lang.reflect.Array
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

object Dynamic {
	@Suppress("UNCHECKED_CAST")
	fun <T> createEmptyClass(clazz: Class<T>): T {
		if (clazz == java.util.Set::class.java) return setOf<Any?>() as T
		if (clazz == java.util.List::class.java) return listOf<Any?>() as T
		if (clazz == java.util.Map::class.java) return mapOf<Any?, Any?>() as T
		if (clazz == java.lang.Iterable::class.java) return listOf<Any?>() as T

		val constructor = clazz.declaredConstructors.firstOrNull() ?: invalidOp("Can't find constructor for class '$clazz'")
		val args = constructor.parameterTypes.map {
			dynamicCast(null, it)
		}
		constructor.isAccessible = true
		return constructor.newInstance(*args.toTypedArray()) as T
	}

	fun <T : Any> setField(instance: T, name: String, value: Any?) {
		val field = instance::class.java.declaredFields.find { it.name == name }
		if (field != null && !Modifier.isStatic(field.modifiers)) {
			//val field = instance.javaClass.getField(name)
			field.isAccessible = true
			field.set(instance, value)
		}
	}

	suspend fun <T : Any> getField(instance: T?, key: String): Any? {
		return if (instance == null) {
			null
		} else {
			// use getAny instead
			//if (instance is Map<*, *>) return instance[key]
			//if (instance is List<*>) {
			//	val index = key.toIntOrNull()
			//	if (index != null) return instance[index]
			//}
			val clazz = instance::class.java
			val dmethods = clazz.declaredMethods
			val getterName = "get${key.capitalize()}"
			val getter = ignoreErrors { dmethods.firstOrNull { it.name == getterName } }
			val method = ignoreErrors { dmethods.firstOrNull { it.name == key } }

			if (getter != null) {
				getter.isAccessible = true
				getter.invokeSuspend(instance, listOf())
			} else if (method != null) {
				method.isAccessible = true
				//method.invoke(instance)
				method.invokeSuspend(instance, listOf())
			} else {
				val field = clazz.declaredFields.find { it.name == key }
				if (field != null && !Modifier.isStatic(field.modifiers)) {
					//val field = instance.javaClass.getField(name)
					field.isAccessible = true
					field.get(instance)
				} else {
					null
				}
			}
		}
	}

	fun <T : Any> getFieldSync(instance: T?, key: String): Any? {
		return if (instance == null) {
			null
		} else {
			// use getAny instead
			//if (instance is Map<*, *>) return instance[key]
			//if (instance is List<*>) {
			//	val index = key.toIntOrNull()
			//	if (index != null) return instance[index]
			//}
			val clazz = instance::class.java
			val dmethods = clazz.declaredMethods
			val getterName = "get${key.capitalize()}"
			val getter = ignoreErrors { dmethods.firstOrNull { it.name == getterName } }
			val method = ignoreErrors { dmethods.firstOrNull { it.name == key } }

			if (getter != null) {
				getter.isAccessible = true
				getter.invoke(instance)
			} else if (method != null) {
				method.isAccessible = true
				//method.invoke(instance)
				method.invoke(instance)
			} else {
				val field = clazz.declaredFields.find { it.name == key }
				if (field != null && !Modifier.isStatic(field.modifiers)) {
					//val field = instance.javaClass.getField(name)
					field.isAccessible = true
					field.get(instance)
				} else {
					null
				}
			}
		}
	}

	fun toNumber(it: Any?): Double {
		return when (it) {
			null -> 0.0
			is Number -> it.toDouble()
			else -> it.toString().toDouble()
		}
	}

	fun toInt(it: Any?): Int {
		return toNumber(it).toInt()
	}

	fun toBool(it: Any?): Boolean {
		return when (it) {
			null -> false
			is Boolean -> it
			is String -> it.isNotEmpty() && it != "0" && it != "false"
			else -> toInt(it) != 0
		}
	}

	fun toBoolOrNull(it: Any?): Boolean? {
		return when (it) {
			null -> null
			is Boolean -> it
			is String -> it.isNotEmpty() && it != "0" && it != "false"
			else -> null
		}
	}

	fun toList(it: Any?): List<*> = toIterable(it).toList()

	fun toIterable(it: Any?): Iterable<*> {
		return when (it) {
			null -> listOf<Any?>()
			is Iterable<*> -> it
			is CharSequence -> it.toList()
			is Map<*, *> -> it.toList()
			else -> listOf<Any?>()
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun toComparable(it: Any?): Comparable<Any?> {
		return when (it) {
			null -> 0 as Comparable<Any?>
			is Comparable<*> -> it as Comparable<Any?>
			else -> it.toString() as Comparable<Any?>
		}
	}

	fun compare(l: Any?, r: Any?): Int {
		if (l is Number && r is Number) {
			return l.toDouble().compareTo(r.toDouble())
		}
		val lc = toComparable(l)
		val rc = toComparable(r)
		if (lc::class.java.isAssignableFrom(rc::class.java)) {
			return lc.compareTo(rc)
		} else {
			return -1
		}
	}

	suspend fun accessAny(instance: Any?, key: Any?): Any? = when (instance) {
		null -> null
		is Map<*, *> -> instance[key]
		is Iterable<*> -> instance.toList()[toInt(key)]
		else -> getField(instance, key.toString())
	}

	fun accessAnySync(instance: Any?, key: Any?): Any? = when (instance) {
		null -> null
		is Map<*, *> -> instance[key]
		is Iterable<*> -> instance.toList()[toInt(key)]
		else -> getFieldSync(instance, key.toString())
	}

	suspend fun getAny(instance: Any?, key: Any?): Any? = accessAny(instance, key)

	fun getAnySync(instance: Any?, key: Any?): Any? = accessAnySync(instance, key)

	@Suppress("UNCHECKED_CAST")
	fun setAny(instance: Any?, key: Any?, value: Any?): Any? {
		return when (instance) {
			null -> null
			is MutableMap<*, *> -> (instance as MutableMap<Any?, Any?>).set(key, value)
			is MutableList<*> -> (instance as MutableList<Any?>)[toInt(key)] = value
			else -> setField(instance, key.toString(), value)
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun setAnySync(instance: Any?, key: Any?, value: Any?): Any? = setAny(instance, key, value)

	fun hasField(javaClass: Class<Any>, name: String): Boolean {
		return javaClass.declaredFields.any { it.name == name }
	}

	fun getFieldType(javaClass: Class<Any>, name: String): Class<*> {
		return javaClass.getField(name).type
	}

	inline fun <reified T : Any> dynamicCast(value: Any?): T? = dynamicCast(value, T::class.java)

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> dynamicCast(value: Any?, target: Class<T>, genericType: Type? = null): T? {
		//if (value != null && target.isAssignableFrom(value.javaClass)) {
		//	return if (genericType != null && genericType is ParameterizedType) {
		//		val typeArgs = genericType.actualTypeArguments
		//		when (value) {
		//			is Set<*> -> value.map { dynamicCast(it, typeArgs[0] as Class<Any>) }.toSet()
		//			is List<*> -> value.map { dynamicCast(it, typeArgs[0] as Class<Any>) }
		//			else -> value
		//		} as T
		//	} else {
		//		value as T
		//	}
		//}
		val str = if (value != null) "$value" else "0"
		if (target.isPrimitive) {
			when (target) {
				java.lang.Boolean.TYPE -> return (str == "true" || str == "1") as T
				Byte.TYPE -> return str.parseInt().toByte() as T
				Character.TYPE -> return if (value is String && value.length >= 1) value[0] as T else str.parseInt().toChar() as T
				java.lang.Short.TYPE -> return str.parseInt().toShort() as T
				java.lang.Long.TYPE -> return str.parseLong() as T
				Float.TYPE -> return str.toFloat() as T
				java.lang.Double.TYPE -> return str.parseDouble() as T
				Integer.TYPE -> return str.parseInt() as T
				else -> invalidOp("Unhandled primitive '${target.name}'")
			}
		}
		if (target.isAssignableFrom(java.lang.Boolean::class.java)) return (str == "true" || str == "1") as T
		if (target.isAssignableFrom(java.lang.Byte::class.java)) return str.parseInt().toByte() as T
		if (target.isAssignableFrom(Character::class.java)) return if (value is String && value.length >= 1) value[0] as T else str.parseInt().toChar() as T
		if (target.isAssignableFrom(java.lang.Short::class.java)) return str.parseShort() as T
		if (target.isAssignableFrom(Integer::class.java)) return str.parseInt() as T
		if (target.isAssignableFrom(java.lang.Long::class.java)) return str.parseLong() as T
		if (target.isAssignableFrom(java.lang.Float::class.java)) return str.toFloat() as T
		if (target.isAssignableFrom(java.lang.Double::class.java)) return str.toDouble() as T
		if (target.isAssignableFrom(java.lang.String::class.java)) return (if (value == null) "" else str) as T
		if (target.isEnum) return if (value != null) Enum.valueOf<AnyEnum>(target as Class<AnyEnum>, str) as T else target.enumConstants.first()
		if (value is Map<*, *>) {
			val map = value as Map<Any?, *>
			val resultClass = target as Class<Any>
			if (resultClass.isAssignableFrom(Map::class.java)) {
				if (genericType is ParameterizedType) {
					val result = hashMapOf<Any?, Any?>()
					val keyType = genericType.actualTypeArguments[0] as? Class<*>?
					val valueType = genericType.actualTypeArguments[1] as? Class<*>?
					for (entry in value.entries) {
						val keyCasted = if (keyType != null) dynamicCast(entry.key, keyType) else entry.key
						val valueCasted = if (valueType != null) dynamicCast(entry.value, valueType) else entry.value
						result[keyCasted] = valueCasted
					}
					return result as T
				} else {
					return map as T
				}
			} else {
				val result = createEmptyClass(resultClass)
				for (field in result::class.java.declaredFields) {
					if (Modifier.isStatic(field.modifiers)) continue
					if (field.name in map) {
						val v = map[field.name]
						field.isAccessible = true
						field.set(result, dynamicCast(v, field.type, field.genericType))
					}
				}
				return result as T
			}
		}
		if (value is Iterable<*>) {
			if (genericType is ParameterizedType) {
				val typeArgs = genericType.actualTypeArguments
				val out = value.map { dynamicCast(it, typeArgs[0] as Class<Any>) }
				return when {
					target.isAssignableFrom(Set::class.java) -> out.toSet() as T
					else -> out as T
				}
			}
			return value.toList() as T
		}
		if (value == null) return createEmptyClass(target)
		invalidOp("Can't convert '$value' to '$target'")
	}

	private enum class AnyEnum {}

	fun String?.parseBool(): Boolean? = when (this) {
		"true", "yes", "1" -> true
		"false", "no", "0" -> false
		else -> null
	}

	fun String?.parseInt(): Int = this?.parseDouble()?.toInt() ?: 0
	fun String?.parseShort(): Short = this?.parseDouble()?.toShort() ?: 0
	fun String?.parseLong(): Long = try {
		this?.toLong()
	} catch (e: Throwable) {
		this?.parseDouble()?.toLong()
	} ?: 0L


	fun String?.parseDouble(): Double {
		if (this == null) return 0.0
		try {
			return this.toDouble()
		} catch (e: Throwable) {
			return 0.0
		}
	}

	fun <T : Any> fromTyped(value: T?): Any? {
		return when (value) {
			null -> null
			true -> true
			false -> false
			is Number -> value.toDouble()
			is String -> value
			is Map<*, *> -> value
			is Iterable<*> -> value
			else -> {
				val clazz = value::class.java
				val out = hashMapOf<Any?, Any?>()
				for (field in clazz.declaredFields) {
					if (Modifier.isStatic(field.modifiers)) continue
					if (field.name.startsWith('$')) continue
					field.isAccessible = true
					out[field.name] = fromTyped(field.get(value))
				}
				out
			}
		}
	}

	fun unop(r: Any?, op: String): Any? {
		return when (op) {
			"+" -> r
			"-" -> -toNumber(r)
			"~" -> toInt(r).inv()
			"!" -> !toBool(r)
			else -> noImpl("Not implemented unary operator $op")
		}
	}

	//fun toFixNumber(value: Double): Any = if (value == value.toInt().toDouble()) value.toInt() else value

	fun toString(value: Any?): String {
		return when (value) {
			null -> ""
			is Double -> {
				if (value == value.toInt().toDouble()) {
					value.toInt().toString()
				} else {
					value.toString()
				}
			}
			is Iterable<*> -> "[" + value.map { toString(it) }.joinToString(", ") + "]"
			is Map<*, *> -> "{" + value.map { toString(it.key).quote() + ": " + toString(it.value) }.joinToString(", ") + "}"
			else -> value.toString()
		}
	}

	fun binop(l: Any?, r: Any?, op: String): Any? {
		return when (op) {
			"+" -> {
				when (l) {
					is String -> l.toString() + toString(r)
					is Iterable<*> -> toIterable(l) + toIterable(r)
					else -> toNumber(l) + toNumber(r)
				}
			}
			"-" -> toNumber(l) - toNumber(r)
			"*" -> toNumber(l) * toNumber(r)
			"/" -> toNumber(l) / toNumber(r)
			"%" -> toNumber(l) % toNumber(r)
			"**" -> Math.pow(toNumber(l), toNumber(r))
			"&" -> toInt(l) and toInt(r)
			"or" -> toInt(l) or toInt(r)
			"^" -> toInt(l) xor toInt(r)
			"&&" -> toBool(l) && toBool(r)
			"||" -> toBool(l) || toBool(r)
			"==" -> {
				if (l is Number && r is Number) {
					l.toDouble() == r.toDouble()
				} else {
					Objects.equals(l, r)
				}
			}
			"!=" -> {
				if (l is Number && r is Number) {
					l.toDouble() != r.toDouble()
				} else {
					!Objects.equals(l, r)
				}
			}
			"<" -> compare(l, r) < 0
			"<=" -> compare(l, r) <= 0
			">" -> compare(l, r) > 0
			">=" -> compare(l, r) >= 0
			"in" -> contains(r, l)
			else -> noImpl("Not implemented binary operator '$op'")
		}
	}

	fun contains(collection: Any?, element: Any?): Boolean = when (collection) {
		is Set<*> -> element in collection
		else -> element in toList(collection)
	}

	suspend fun callAny(obj: Any?, key: Any?, args: List<Any?>): Any? {
		if (obj == null || key == null) return null
		val method = obj::class.java.methods.first { it.name == key }
		method.isAccessible = true
		val result = method.invokeSuspend(obj, args)
		return result
	}

	suspend fun callAny(callable: Any?, args: List<Any?>): Any? {
		return callAny(callable, "invoke", args)
	}

	fun length(subject: Any?): Int {
		if (subject == null) return 0
		if (subject::class.java.isArray) return Array.getLength(subject)
		if (subject is List<*>) return subject.size
		if (subject is Map<*, *>) return subject.size
		if (subject is Iterable<*>) return subject.count()
		return subject.toString().length
	}

	interface Context {
		fun Any?.toDynamicString() = Dynamic.toString(this)
		fun Any?.toDynamicBool() = Dynamic.toBool(this)
		fun Any?.toDynamicInt() = Dynamic.toInt(this)
		fun Any?.toDynamicList() = Dynamic.toList(this)
		fun Any?.toDynamicIterable() = Dynamic.toIterable(this)
		fun Any?.dynamicLength() = Dynamic.length(this)
		suspend fun Any?.dynamicGet(key: Any?) = Dynamic.accessAny(this, key)
		suspend fun Any?.dynamicSet(key: Any?, value: Any?) = Dynamic.setAny(this, key, value)
		suspend fun Any?.dynamicCall(vararg args: Any?) = Dynamic.callAny(this, args.toList())
		suspend fun Any?.dynamicCallMethod(methodName: Any?, vararg args: Any?) = Dynamic.callAny(this, methodName, args.toList())
		suspend fun Any?.dynamicCastTo(target: Class<*>) = Dynamic.dynamicCast(this, target)
	}

	val contextInstance: Context = object : Context {}

	inline fun context(callback: Context.() -> Unit) {
		contextInstance.callback()
	}
}
