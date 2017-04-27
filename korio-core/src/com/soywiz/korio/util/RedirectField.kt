package com.soywiz.korio.util

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class RedirectField<V>(val redirect: KProperty0<V>) {
	inline operator fun getValue(thisRef: Any, property: KProperty<*>): V {
		return redirect.get()
	}
}

class RedirectMutableField<V>(val redirect: KMutableProperty0<V>) {
	inline operator fun getValue(thisRef: Any, property: KProperty<*>): V {
		return redirect.get()
	}

	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: V) {
		redirect.set(value)
	}
}

class RedirectMutableFieldGen<V>(val redirect: () -> KMutableProperty0<V>) {
	inline operator fun getValue(thisRef: Any, property: KProperty<*>): V {
		return redirect().get()
	}

	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: V) {
		redirect().set(value)
	}
}

inline fun <V> redirectField(noinline redirect: () -> KMutableProperty0<V>) = RedirectMutableFieldGen(redirect)

inline fun <V> redirectField(redirect: KMutableProperty0<V>) = RedirectMutableField(redirect)
inline fun <V> redirectField(redirect: KProperty0<V>) = RedirectField(redirect)

inline fun <V> KMutableProperty0<V>.redirect() = RedirectMutableField(this)
inline fun <V> KProperty0<V>.redirect() = RedirectField(this)
