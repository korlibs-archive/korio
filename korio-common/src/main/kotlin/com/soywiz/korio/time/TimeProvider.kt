package com.soywiz.korio.time

import com.soywiz.korio.KorioNative

open class TimeProvider {
	open fun currentTimeMillis() = KorioNative.currentTimeMillis()

	companion object {
		fun now() = KorioNative.currentTimeMillis()

		operator fun invoke(callback: () -> Long) = object : TimeProvider() {
			override fun currentTimeMillis(): Long = callback()
		}
	}
}