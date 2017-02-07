package com.soywiz.korio.util

open class TimeProvider {
	open fun currentTimeMillis() = System.currentTimeMillis()

	companion object {
		operator fun invoke(callback: () -> Long) = object : TimeProvider() {
			override fun currentTimeMillis(): Long = callback()
		}
	}
}