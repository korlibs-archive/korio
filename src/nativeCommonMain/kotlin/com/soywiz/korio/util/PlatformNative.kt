package com.soywiz.korio.util

@PublishedApi
internal actual val Platform_current get() = Platform.NATIVE

actual object PlatformInfo {
	actual val platformName: String get() = "native"
	actual val rawOsName: String = com.soywiz.korio.TARGET_INFO
}
