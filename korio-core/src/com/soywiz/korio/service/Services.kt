package com.soywiz.korio.service

import com.soywiz.korio.error.invalidOp
import java.util.*

object Services {
	open class Impl {
		open val available: Boolean = true
		open val priority: Int = 5000
	}

	inline fun <reified T : Impl> load(): T = load(T::class.java)

	fun <T : Impl> load(clazz: Class<T>): T = ServiceLoader.load(clazz)
			.filter { it.available }
			.sortedBy { it.priority }
			.firstOrNull() ?: invalidOp("Can't find implementation for ${clazz.name}")
}