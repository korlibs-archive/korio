package com.soywiz.korio.lang

import kotlin.reflect.KProperty

abstract class ThreadLocal<T> {
	abstract fun initialValue(): T
	private var value = initialValue()
	fun get(): T = value
	fun set(value: T) = run { this.value = value }
}

class threadLocal<T>(val gen: () -> T) {
	val local = object : ThreadLocal<T>() {
		override fun initialValue(): T = gen()
	}

	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T = local.get()
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit = local.set(value)
}