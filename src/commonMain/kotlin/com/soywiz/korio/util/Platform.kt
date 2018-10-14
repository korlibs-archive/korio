package com.soywiz.korio.util

@PublishedApi
internal expect val Platform_current: Platform

enum class Platform {
	NATIVE, JS, JVM, UNKNOWN;

	companion object {
	    inline val CURRENT get() = Platform_current

		inline val isJvm get() = Platform_current == Platform.JVM
		inline val isJs get() = Platform_current == Platform.JS
		inline val isNative get() = Platform_current == Platform.NATIVE
	}
}
