package com.soywiz.korio.util

import kotlin.reflect.KProperty

interface Extra {
	var extra: HashMap<String, Any?>?

	class Mixin(override var extra: HashMap<String, Any?>? = null) : Extra
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
class extraProperty<T : Any?>(val name: String, val default: T) {
	inline operator fun getValue(thisRef: Extra, property: KProperty<*>): T = (thisRef.extra?.get(name) as T?) ?: default
	inline operator fun setValue(thisRef: Extra, property: KProperty<*>, value: T): Unit = run {
		if (thisRef.extra == null) thisRef.extra = hashMapOf()
		thisRef.extra?.set(name, value as Any?)
	}
}
