package com.soywiz.korio.util

open class TimeProvider {
	open fun currentTimeMillis() = com.soywiz.korio.time.currentTimeMillis()

	companion object {
		operator fun invoke(callback: () -> Long) = object : TimeProvider() {
			override fun currentTimeMillis(): Long = callback()
		}
	}
}