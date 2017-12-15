package com.soywiz.korio.lang

object DynamicAccess {
	operator inline fun <T> invoke(callback: DynamicAccess.() -> T): T = callback(DynamicAccess)

	val Any?.list: List<Any?>
		get() = when (this) {
			is List<*> -> this
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
	val Any?.float: Float get() = toFloatDefault()
	val Any?.double: Double get() = toDoubleDefault()
	val Any?.long: Long get() = toLongDefault()

	val Any?.intArray: IntArray get() = list.map { it.int }.toIntArray()
	val Any?.floatArray: FloatArray get() = list.map { it.float }.toFloatArray()
	val Any?.doubleArray: DoubleArray get() = list.map { it.double }.toDoubleArray()
	val Any?.longArray: LongArray get() = list.map { it.long }.toLongArray()
}
