package com.soywiz.korio.lang

abstract class ThreadLocal<T> {
	abstract fun initialValue(): T
	private var value = initialValue()
	fun get(): T = value
	fun set(value: T) = run { this.value = value }
}
