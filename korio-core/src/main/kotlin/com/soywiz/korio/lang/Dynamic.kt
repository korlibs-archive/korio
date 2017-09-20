package com.soywiz.korio.lang

object Dynamic {
	fun getAnySync(obj: Any?, key: String): Any? = TODO()
	fun toString(obj: Any?): String = TODO()
	fun <T> dynamicCast(obj: Any?, clazz: KClass<T>): T? = TODO()
	fun toInt(obj: Any?): Int = TODO()
	fun toDouble(obj: Any?): Double = TODO()
	fun toBool(obj: Any?): Boolean = TODO()
	fun toList(obj: Any?): List<Any?> = TODO()
}