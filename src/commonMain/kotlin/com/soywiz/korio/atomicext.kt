package com.soywiz.korio

import kotlinx.atomicfu.*
import kotlin.reflect.*

internal inline operator fun <T> AtomicRef<T>.getValue(obj: Any, property: KProperty<Any?>): T = this.value
internal inline operator fun <T> AtomicRef<T>.setValue(obj: Any, property: KProperty<Any?>, value: T) = run { this.value = value }

internal inline operator fun AtomicBoolean.getValue(obj: Any, property: KProperty<Any?>): Boolean = this.value
internal inline operator fun AtomicBoolean.setValue(obj: Any, property: KProperty<Any?>, value: Boolean) = run { this.value = value }

internal inline operator fun AtomicInt.getValue(obj: Any, property: KProperty<Any?>): Int = this.value
internal inline operator fun AtomicInt.setValue(obj: Any, property: KProperty<Any?>, value: Int) = run { this.value = value }

internal inline operator fun AtomicLong.getValue(obj: Any, property: KProperty<Any?>): Long = this.value
internal inline operator fun AtomicLong.setValue(obj: Any, property: KProperty<Any?>, value: Long) = run { this.value = value }
