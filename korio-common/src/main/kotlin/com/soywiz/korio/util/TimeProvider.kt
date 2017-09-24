package com.soywiz.korio.util

import com.soywiz.korio.time.STimeProvider

open class TimeProvider {
	open fun currentTimeMillis() = STimeProvider.currentTimeMillis()

	companion object {
		operator fun invoke(callback: () -> Long) = object : TimeProvider() {
			override fun currentTimeMillis(): Long = callback()
		}
	}
}