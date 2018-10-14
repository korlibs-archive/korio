package com.soywiz.korio.concurrent

actual class Lock actual constructor() {
	actual inline operator fun <T> invoke(callback: () -> T): T = synchronized(this) { callback() }
}
