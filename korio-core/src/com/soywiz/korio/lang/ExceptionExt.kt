package com.soywiz.korio.lang

fun Throwable.printStackTrace() {
	// @TODO: Implement in each platform!
	Console.error(this.message ?: "Error")
}