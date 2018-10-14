package com.soywiz.korio

fun nativeCwd(): String = platform.Foundation.NSBundle.mainBundle.resourcePath ?: "."
