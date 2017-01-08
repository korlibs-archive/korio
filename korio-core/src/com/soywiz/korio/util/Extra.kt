package com.soywiz.korio.util

import kotlin.reflect.KProperty

open interface Extra {
	val extra: HashMap<String, Any?>
	class Mixin(override val extra: HashMap<String, Any?> = HashMap()) : Extra
}

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
class extraProperty<T : Any?>(val name: String, val default: T) {
	inline operator fun getValue(thisRef: Extra, property: KProperty<*>): T = (thisRef.extra[name] as T?) ?: default
	inline operator fun setValue(thisRef: Extra, property: KProperty<*>, value: T): Unit = run { thisRef.extra[name] = value as Any? }
}
