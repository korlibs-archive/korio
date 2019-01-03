package com.soywiz.korio.lang

import com.soywiz.korio.*

expect fun Throwable.printStackTrace()

fun printStackTrace() {
	Exception("printStackTrace").printStackTrace()
}
