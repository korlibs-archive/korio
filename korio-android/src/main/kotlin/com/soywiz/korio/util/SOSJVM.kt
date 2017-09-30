package com.soywiz.korio.util

actual object SOS {
	actual val name: String = System.getProperty("os.name")
}