package com.soywiz.korio.util

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class Computed<K : Computed.WithParent<K>, T>(val prop: KProperty1<K, T?>, val default: () -> T) {
	interface WithParent<T> {
		val parent: T?
	}

	operator fun getValue(thisRef: K, p: KProperty<*>): T {
		val par = thisRef.parent
		val r = prop.get(thisRef)
		if (r != null) return r

		if (par != null) {
			val v = prop.get(par)
			if (v != null) return v
		}

		return default()
	}
}
