package com.soywiz.korio.util

@PublishedApi
internal expect val Platform_current: Platform

enum class Platform {
	NATIVE, JS, JVM, UNKNOWN;

	companion object
}

inline val Platform.Companion.CURRENT get() = Platform_current

inline val Platform.Companion.isJvm get() = Platform_current == Platform.JVM
inline val Platform.Companion.isJs get() = Platform_current == Platform.JS
inline val Platform.Companion.isNative get() = Platform_current == Platform.NATIVE
