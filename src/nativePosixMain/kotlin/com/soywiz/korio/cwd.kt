package com.soywiz.korio

import kotlinx.cinterop.*
import platform.posix.*

fun nativeCwd(): String = platform.Foundation.NSBundle.mainBundle.resourcePath ?: "."

fun doMkdir(path: String, attr: Int): Int {
	return platform.posix.mkdir(path, attr.toUShort())
}

fun realpath(path: String): String = memScoped {
	val temp = allocArray<ByteVar>(PATH_MAX)
	realpath(path, temp)
	temp.toKString()
}
