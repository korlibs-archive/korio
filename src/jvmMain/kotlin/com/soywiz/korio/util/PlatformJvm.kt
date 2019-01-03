package com.soywiz.korio.util

@PublishedApi
internal actual val Platform_current get() = Platform.JVM

actual object PlatformInfo {
	actual val platformName: String = "jvm"
	actual val rawOsName: String by lazy { System.getProperty("os.name") }
}
