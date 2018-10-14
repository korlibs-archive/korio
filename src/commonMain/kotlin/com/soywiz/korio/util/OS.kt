package com.soywiz.korio.util

import com.soywiz.korio.*

object OS {
	val rawName = KorioNative.rawOsName
	val rawNameLC = rawName.toLowerCase()

	val platformName = KorioNative.platformName
	val platformNameLC = platformName.toLowerCase()

	val isWindows = rawNameLC.contains("win")
	val isUnix = !isWindows
	val isPosix = !isWindows
	val isLinux = rawNameLC.contains("nix") || rawNameLC.contains("nux") || rawNameLC.contains("aix")
	val isMac = rawNameLC.contains("mac")
	val isNodejs = (platformNameLC.contains("node.js"))

	val isBrowserJs = isJs && !isNodejs

	inline val isJs get() = Platform.isJs
	inline val isNative get() = Platform.isNative
	inline val isJvm get() = Platform.isJvm
}
