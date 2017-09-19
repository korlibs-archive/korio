package com.soywiz.korio.lang

fun Throwable.printStackTrace() {
	// @TODO: Implement in each platform!
	println(this.message)
}