package com.soywiz.korio.lang.tl

import com.soywiz.korio.lang.ThreadLocal
import kotlin.reflect.KProperty

class threadLocal<T>(val gen: () -> T) {
	val local = object : ThreadLocal<T>() {
		override fun initialValue(): T = gen()
	}

	inline operator fun getValue(thisRef: Any?, property: KProperty<*>): T = local.get()
	inline operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit = local.set(value)
}