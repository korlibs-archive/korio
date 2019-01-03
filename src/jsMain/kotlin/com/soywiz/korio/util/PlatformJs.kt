package com.soywiz.korio.util

import com.soywiz.korio.*

@PublishedApi
internal actual val Platform_current get() = Platform.JS

actual object PlatformInfo {
	actual val platformName: String
		get() = when {
			isWeb -> "web.js"
			isNodeJs -> "node.js"
			isWorker -> "worker.js"
			isShell -> "shell.js"
			else -> "js"
		}

	actual val rawOsName: String = when {
		isNodeJs -> process.platform
		else -> navigator.platform
	}
}