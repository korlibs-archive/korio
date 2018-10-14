package com.soywiz.korio.concurrent

// In JS and Native this is not required
actual class Lock actual constructor() {
	actual inline operator fun <T> invoke(callback: () -> T): T = callback()
}
