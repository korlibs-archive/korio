package com.soywiz.korio.util

import kotlin.reflect.KProperty

class threadLocal<T>(val gen: () -> T) {
	val local = ThreadLocal.withInitial(gen)
	operator fun getValue(thisRef: Any?, property: KProperty<*>): T = local.get()
}