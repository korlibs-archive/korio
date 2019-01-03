package com.soywiz.korio.util.i18n

import com.soywiz.korio.*
import kotlin.browser.*

// @NOTE: Important to keep return value or it will believe that this is a List<dynamic>
internal actual val systemLanguageStrings: List<String> by lazy {
	if (isNodeJs) {
		val env = process.env
		listOf<String>(env.LANG ?: env.LANGUAGE ?: env.LC_ALL ?: env.LC_MESSAGES ?: "english")
	} else {
		//console.log("window.navigator.languages", window.navigator.languages)
		//console.log("window.navigator.languages", window.navigator.languages.toList())
		window.navigator.languages.asList()
		//val langs = window.navigator.languages
		//(0 until langs.size).map { "" + langs[it] + "-" }
	}
}

