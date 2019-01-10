package com.soywiz.korio.dynamic

internal expect object DynamicInternal {
	val global: Any?
	fun get(instance: Any?, key: String): Any?
	fun invoke(instance: Any?, key: String, args: Array<out Any?>): Any?
}

object KDynamic {
	inline operator fun <T> invoke(callback: KDynamic.() -> T): T = callback(KDynamic)
	val global get() = DynamicInternal.global
	operator fun Any?.get(key: String): Any? = DynamicInternal.get(this, key)
	fun Any?.dynamicInvoke(name: String, vararg args: Any?): Any? = DynamicInternal.invoke(this, name, args)
}
