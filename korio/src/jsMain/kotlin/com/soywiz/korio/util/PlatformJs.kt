package com.soywiz.korio.util

import com.soywiz.korio.*

internal actual val rawOsName: String = when {
	isNodeJs -> process.platform
	else -> navigator.platform
}

internal actual val rawPlatformName: String = when {
	isWeb -> "web.js"
	isNodeJs -> "node.js"
	isWorker -> "worker.js"
	isShell -> "shell.js"
	else -> "js"
}
