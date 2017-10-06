package com.soywiz.korio.util

expect object SOS {
	val name: String
}

object OS {
	val name by lazy { SOS.name }
	val nameLC by lazy { name.toLowerCase() }

	val isWindows by lazy { nameLC.contains("win") }
	val isUnix by lazy { !isWindows }
	val isLinux by lazy { nameLC.contains("nix") || nameLC.contains("nux") || nameLC.contains("aix") }
	val isMac by lazy { nameLC.contains("mac") }

	val isJs: Boolean by lazy { nameLC.contains("js") }
	val isNodejs: Boolean by lazy { (nameLC.contains("node.js")) }
	val isBrowserJs: Boolean get() = isJs && !isNodejs
}