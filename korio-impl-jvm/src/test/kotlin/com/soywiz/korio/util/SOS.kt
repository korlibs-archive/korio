package com.soywiz.korio.util

impl object SOS {
	impl val name: String get() = System.getProperty("os.name")
}