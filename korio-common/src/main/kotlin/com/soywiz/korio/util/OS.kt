package com.soywiz.korio.util

header object SOS {
	val name: String
}

object OS {
	val name = SOS.name

	val isWindows by lazy { name.contains("win") }
	val isUnix by lazy { !isWindows }
	val isLinux by lazy { name.contains("nix") || name.contains("nux") || name.contains("aix") }
	val isMac by lazy { name.contains("mac") }

	val isJs: Boolean get() = (name.contains("js"))

	val isNodejs: Boolean get() = (name.contains("node.js"))
	val isBrowserJs: Boolean get() = isJs && !isNodejs
}