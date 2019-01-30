package com.soywiz.korio.util

internal expect val rawPlatformName: String
internal expect val rawOsName: String

object OS {
	val rawName = rawOsName
	val rawNameLC = rawName.toLowerCase()

	val platformName = rawPlatformName
	val platformNameLC = platformName.toLowerCase()

	val isWindows = rawNameLC.contains("win")
	val isUnix = !isWindows
	val isPosix = !isWindows
	val isLinux = rawNameLC.contains("nix") || rawNameLC.contains("nux") || rawNameLC.contains("aix")
	val isMac = rawNameLC.contains("mac")

	val isIos = rawNameLC.contains("ios")
	val isAndroid = platformNameLC.contains("android")

	val isJs = rawPlatformName.endsWith("js")
	val isNative = rawPlatformName == "native"
	val isJvm = rawPlatformName == "jvm"

	val isJsShell = platformNameLC == "shell.js"
	val isJsNodeJs = platformNameLC == "node.js"
	val isJsBrowser = platformNameLC == "web.js"
	val isJsWorker = platformNameLC == "worker.js"
	val isJsBrowserOrWorker = isJsBrowser || isJsWorker
}
