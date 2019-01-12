package com.soywiz.korio.util

import kotlin.reflect.*

class RedirectField<V>(val redirect: KProperty0<V>) {
	inline operator fun getValue(thisRef: Any, property: KProperty<*>): V = redirect.get()
}

class RedirectMutableField<V>(val redirect: KMutableProperty0<V>) {
	inline operator fun getValue(thisRef: Any, property: KProperty<*>): V = redirect.get()
	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: V) = redirect.set(value)
}

class RedirectMutableFieldGen<V>(val redirect: () -> KMutableProperty0<V>) {
	inline operator fun getValue(thisRef: Any, property: KProperty<*>): V = redirect().get()
	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: V) = redirect().set(value)
}

class RedirectFieldGen<V>(val redirect: () -> KProperty0<V>) {
	inline operator fun getValue(thisRef: Any, property: KProperty<*>): V = redirect().get()
}

inline fun <V> (() -> KProperty0<V>).redirected() = RedirectFieldGen(this)
inline fun <V> (() -> KMutableProperty0<V>).redirected() = RedirectMutableFieldGen(this)
inline fun <V> KMutableProperty0<V>.redirected() = RedirectMutableField(this)
inline fun <V> KProperty0<V>.redirected() = RedirectField(this)
