package com.soywiz.korio.inject

fun AsyncInjector.jvmAutomapping(): AsyncInjector = this.apply {
	this.defaultProvider = { clazz ->

	}
}