package com.soywiz.korio.lang

class AtomicLong(var value: Long = 0) {
	fun get() = value
	fun incrementAndGet() = ++value
	fun decrementAndGet() = --value
}

class AtomicInteger(var value: Int = 0) {
	fun get() = value
	fun incrementAndGet() = ++value
	fun decrementAndGet() = --value
}