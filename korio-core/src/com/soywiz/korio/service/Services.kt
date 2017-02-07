package com.soywiz.korio.service

import com.soywiz.korio.error.invalidOp
import java.util.*

object Services {
	open class Impl {
		open val available: Boolean = true
		open val priority: Int = 5000
	}

	inline fun <reified T : Impl> load(): T = ServiceLoader.load(T::class.java)
			.filter { it.available }
			.sortedBy { it.priority }
			.firstOrNull() ?: invalidOp("Can't find implementation for ${T::class.java.name}")
}