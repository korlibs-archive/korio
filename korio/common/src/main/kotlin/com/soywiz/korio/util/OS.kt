package com.soywiz.korio.util

import com.soywiz.korio.KorioNative

object OS {
	val rawName = KorioNative.rawOsName
	val rawNameLC = rawName.toLowerCase()

	val platformName = KorioNative.platformName
	val platformNameLC = platformName.toLowerCase()

	val isWindows by lazy { rawNameLC.contains("win") }
	val isUnix by lazy { !isWindows }
	val isLinux by lazy { rawNameLC.contains("nix") || rawNameLC.contains("nux") || rawNameLC.contains("aix") }
	val isMac by lazy { rawNameLC.contains("mac") }

	val isJs: Boolean by lazy { platformNameLC.contains("js") }
	val isNodejs: Boolean by lazy { (platformNameLC.contains("node.js")) }
	val isBrowserJs: Boolean get() = isJs && !isNodejs
}
