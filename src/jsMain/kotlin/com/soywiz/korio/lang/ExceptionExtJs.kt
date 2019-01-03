package com.soywiz.korio.lang

actual fun Throwable.printStackTrace() {
	val e = this
	console.error(e.asDynamic())
	console.error(e.asDynamic().stack)
}