package com.soywiz.korio.time

import com.soywiz.klock.Klock

open class TimeProvider {
	open fun currentTimeMillis() = Klock.currentTimeMillis()

	companion object {
		fun now() = Klock.currentTimeMillis()

		operator fun invoke(callback: () -> Long) = object : TimeProvider() {
			override fun currentTimeMillis(): Long = callback()
		}
	}
}