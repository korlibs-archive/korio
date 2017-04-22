package com.soywiz.korio.util

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class RedirectField<V>(val redirect: KProperty0<V>) {
	operator fun getValue(thisRef: Any, property: KProperty<*>): V {
		return redirect.get()
	}
}

class RedirectMutableField<V>(val redirect: KMutableProperty0<V>) {
	operator fun getValue(thisRef: Any, property: KProperty<*>): V {
		return redirect.get()
	}

	operator fun setValue(thisRef: Any, property: KProperty<*>, value: V) {
		redirect.set(value)
	}
}

fun <V> redirectField(redirect: KMutableProperty0<V>) = RedirectMutableField(redirect)
fun <V> redirectField(redirect: KProperty0<V>) = RedirectField(redirect)

fun <V> KMutableProperty0<V>.redirect() = RedirectMutableField(this)
fun <V> KProperty0<V>.redirect() = RedirectField(this)
