package com.soywiz.korio.lang

// @TODO: Optimize without synchronized per platform!
class AtomicLong(private var value: Long = 0) {
	fun get() = synchronized(this) { value }
	fun incrementAndGet() = synchronized(this) { ++value }
	fun decrementAndGet() = synchronized(this) { --value }
	fun addAndGet(i: Long): Long = synchronized(this) {
		value += i
		value
	}
}

// @TODO: Optimize without synchronized per platform!
class AtomicInteger(private var value: Int = 0) {
	fun get() = synchronized(this) { value }
	fun incrementAndGet() = synchronized(this) { ++value }
	fun decrementAndGet() = synchronized(this) { --value }
	fun addAndGet(i: Int): Int = synchronized(this) {
		value += i
		value
	}
}