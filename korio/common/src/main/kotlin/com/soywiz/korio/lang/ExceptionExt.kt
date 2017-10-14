package com.soywiz.korio.lang

import com.soywiz.korio.KorioNative

fun Throwable.printStackTrace() {
	KorioNative.printStackTrace(this)
}