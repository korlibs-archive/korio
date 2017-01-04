package com.soywiz.korio.android

import android.app.Activity

private var _KorioAndroidContext: Activity? = null
var KorioAndroidContext: Activity
	get() = _KorioAndroidContext ?: throw IllegalStateException("Must call KorioAndroidInit first!")
	private set(value) {
		_KorioAndroidContext = value
	}

fun KorioAndroidInit(activity: Activity) {
	KorioAndroidContext = activity
}