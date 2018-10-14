package com.soywiz.korio.lang

import com.soywiz.korio.serialization.*

object Dynamic {
	inline operator fun <T> invoke(callback: DynamicAccess.() -> T): T = DynamicAccess(callback)
	inline operator fun <T, T2> invoke(value: T2, callback: DynamicAccess.(T2) -> T): T = DynamicAccess.run { callback(this, value) }

	fun set(obj: Any?, key: Any?, value: Any?): Unit = when (obj) {
		is MutableMap<*, *>, is MutableList<*> -> setUntyped(obj, key, value)
		else -> Unit
	}

	fun get(obj: Any?, key: Any?): Any? = when (obj) {
		null, is Map<*, *>, is List<*> -> getUntyped(obj, key)
		else -> getUntyped(Mapper.toUntyped(obj), key)
	}

	fun setUntyped(obj: Any?, key: Any?, value: Any?): Unit = when (obj) {
		null -> Unit
		is MutableMap<*, *> -> (obj as MutableMap<Any?, Any?>)[key] = value
		is MutableList<*> -> (obj as MutableList<Any?>)[toInt(key)] = value
		else -> Unit
	}

	fun getUntyped(obj: Any?, key: Any?): Any? = when (obj) {
		null -> null
		is Map<*, *> -> obj[key]
		is List<*> -> obj[toInt(key)]
		else -> null
	}

	fun toStringOrNull(obj: Any?): String? = obj?.toString()

	fun toString(obj: Any?): String = toStringOrNull(obj) ?: "null"

	fun toList(obj: Any?): List<Any?> = when (obj) {
		null -> listOf()
		is List<*> -> obj
		is Iterable<*> -> obj.toList()
		else -> listOf(obj)
	}

	fun toMap(obj: Any?): Map<Any?, Any?> = when (obj) {
		null -> LinkedHashMap()
		is Map<*, *> -> obj as Map<Any?, Any?>
		else -> LinkedHashMap()
	}

	fun toNumber(obj: Any?): Number = when (obj) {
		null -> 0
		is Boolean -> if (obj) 1 else 0
		is Number -> obj
		//is String -> (obj.toLongOrNull() as? Number?) ?: obj.toIntOrNull() ?: obj.toDoubleOrNull() ?: 0
		is String -> (obj.toIntOrNull() ?: obj.toDoubleOrNull() ?: 0) as Number
		else -> 0
	}

	fun toBool(obj: Any?): Boolean = when (obj) {
		is Boolean -> obj
		is String -> when (obj.toLowerCase()) {
		//"1", "true", "ok", "yes" -> true
			"", "0", "false", "ko", "no" -> false
		//else -> false
			else -> true
		}
		else -> toInt(obj) != 0
	}

	fun toByte(obj: Any?): Byte = toNumber(obj).toByte()
	fun toChar(obj: Any?): Char = when {
		obj is Char -> obj
		(obj is String) && (obj.length == 1) -> obj.first()
		else -> toNumber(obj).toChar()
	}

	fun toShort(obj: Any?): Short = toNumber(obj).toShort()
	fun toInt(obj: Any?): Int = toNumber(obj).toInt()
	fun toLong(obj: Any?): Long = toNumber(obj).toLong()
	fun toFloat(obj: Any?): Float = toNumber(obj).toFloat()
	fun toDouble(obj: Any?): Double = toNumber(obj).toDouble()
}

interface DynamicContext {
	fun Any?.toBool() = Dynamic.toBool(this)
	fun Any?.toByte() = Dynamic.toByte(this)
	fun Any?.toChar() = Dynamic.toChar(this)
	fun Any?.toShort() = Dynamic.toShort(this)
	fun Any?.toInt() = Dynamic.toInt(this)
	fun Any?.toLong() = Dynamic.toLong(this)
	fun Any?.toFloat() = Dynamic.toFloat(this)
	fun Any?.toDouble() = Dynamic.toDouble(this)
	fun Any?.toNumber() = Dynamic.toNumber(this)
	fun Any?.toDynamicList() = Dynamic.toList(this)
	fun Any?.toDynamicMap() = Dynamic.toMap(this)
	operator fun Any?.get(key: Any?) = Dynamic.get(this, key)
}

object DynamicContextInstance : DynamicContext

object DynamicAccess {
	inline operator fun <T> invoke(callback: DynamicAccess.() -> T): T = callback(DynamicAccess)

	val Any?.list: List<Any?>
		get() = when (this) {
			is List<*> -> this
			is Iterable<*> -> this.toList()
			else -> listOf()
		}

	val Any?.keys: List<Any?>
		get() = when (this) {
			is Map<*, *> -> keys.toList()
			else -> listOf()
		}

	operator fun Any?.get(key: String): Any? = when (this) {
		is Map<*, *> -> (this as Map<String, *>)[key]
		else -> null
	}

	operator fun Any?.get(key: Int): Any? = when (this) {
		is List<*> -> this[key]
		else -> null
	}

	fun Any?.toBoolOrNull(): Boolean? = when (this) {
		is String -> this == "1" || this == "true" || this == "on"
		is Number -> toInt() != 0
		else -> null
	}

	fun Any?.toIntOrNull(): Int? = when (this) {
		is Number -> toInt()
		is String -> this.toIntOrNull(10)
		else -> null
	}

	fun Any?.toLongOrNull(): Long? = when (this) {
		is Number -> toLong()
		is String -> toLongOrNull(10)
		else -> null
	}

	fun Any?.toDoubleOrNull(): Double? = when (this) {
		is Number -> toDouble()
		is String -> this.toDouble()
		else -> null
	}

	fun Any?.toIntDefault(default: Int = 0): Int = when (this) {
		is Number -> toInt()
		is String -> toIntOrNull(10) ?: default
		else -> default
	}

	fun Any?.toLongDefault(default: Long = 0L): Long = when (this) {
		is Number -> toLong()
		is String -> toLongOrNull(10) ?: default
		else -> default
	}

	fun Any?.toFloatDefault(default: Float = 0f): Float = when (this) {
		is Number -> toFloat()
		is String -> this.toFloat()
		else -> default
	}

	fun Any?.toDoubleDefault(default: Double = 0.0): Double = when (this) {
		is Number -> toDouble()
		is String -> this.toDouble()
		else -> default
	}

	val Any?.str: String get() = toString()
	val Any?.int: Int get() = toIntDefault()
	val Any?.bool: Boolean get() = toBoolOrNull() ?: false
	val Any?.float: Float get() = toFloatDefault()
	val Any?.double: Double get() = toDoubleDefault()
	val Any?.long: Long get() = toLongDefault()

	val Any?.intArray: IntArray get() = this as? IntArray ?: list.map { it.int }.toIntArray()
	val Any?.floatArray: FloatArray get() = this as? FloatArray ?: list.map { it.float }.toFloatArray()
	val Any?.doubleArray: DoubleArray get() = this as? DoubleArray ?: list.map { it.double }.toDoubleArray()
	val Any?.longArray: LongArray get() = this as? LongArray ?: list.map { it.long }.toLongArray()
}
