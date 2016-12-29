package com.soywiz.korio.util

object OS {
	val name by lazy { System.getProperty("os.name").toLowerCase() }

	val isUnix by lazy { name.contains("nix") || name.contains("nux") || name.contains("aix") }
	val isWindows by lazy { name.contains("win") }
	val isMac by lazy { name.contains("mac") }
}