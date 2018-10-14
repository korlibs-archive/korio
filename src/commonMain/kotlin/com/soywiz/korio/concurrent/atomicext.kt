package com.soywiz.korio.concurrent

import kotlinx.atomicfu.*
import kotlin.reflect.*

inline operator fun <T> AtomicRef<T>.getValue(obj: Any, prop: KProperty<Any?>): T = this.value
inline operator fun <T> AtomicRef<T>.setValue(obj: Any, prop: KProperty<Any?>, v: T) = run { this.value = v }

inline operator fun AtomicBoolean.getValue(obj: Any, prop: KProperty<Any?>): Boolean = this.value
inline operator fun AtomicBoolean.setValue(obj: Any, prop: KProperty<Any?>, v: Boolean) = run { this.value = v }

inline operator fun AtomicInt.getValue(obj: Any, prop: KProperty<Any?>): Int = this.value
inline operator fun AtomicInt.setValue(obj: Any, prop: KProperty<Any?>, v: Int) = run { this.value = v }

inline operator fun AtomicLong.getValue(obj: Any, prop: KProperty<Any?>): Long = this.value
inline operator fun AtomicLong.setValue(obj: Any, prop: KProperty<Any?>, v: Long) = run { this.value = v }
