package com.soywiz.korio.util

import com.jtransc.annotation.JTranscMethodBody

object OS {
	val name by lazy { System.getProperty("os.name").toLowerCase() }

	val isUnix by lazy { name.contains("nix") || name.contains("nux") || name.contains("aix") }
	val isWindows by lazy { name.contains("win") }
	val isMac by lazy { name.contains("mac") }

	val isJs: Boolean @JTranscMethodBody(target = "js", value = "return true;") get() = false

	val isNodejs: Boolean @JTranscMethodBody(target = "js", value = "return (typeof module !== 'undefined' && module.exports);") get() = false
}