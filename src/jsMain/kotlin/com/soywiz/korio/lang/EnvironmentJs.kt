package com.soywiz.korio.lang

import com.soywiz.korio.*
import com.soywiz.korio.net.*
import com.soywiz.korio.util.*
import kotlin.browser.*

actual object Environment {
	actual operator fun get(key: String): String? = if (OS.isJsNodeJs) {
		process.env[key]
	} else {
		val qs = QueryString.decode((document.location?.search ?: "").trimStart('?'))
		val envs = qs.map { it.key.toUpperCase() to (it.value.firstOrNull() ?: it.key) }.toMap()
		envs[key.toUpperCase()]
	}
}
