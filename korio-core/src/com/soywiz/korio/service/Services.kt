package com.soywiz.korio.service

import com.soywiz.korio.error.invalidOp
import java.util.*

object Services {
	open class Impl {
		open val available: Boolean = true
		open val priority: Int = 5000
	}

	private val implsClasses = hashSetOf<Pair<Class<*>, Class<*>>>()
	private val impls = hashMapOf<Class<out Services.Impl>, ArrayList<Services.Impl>>()

	fun <T : Services.Impl> register(interfase: Class<T>, impl: Class<out T>) {
		val pair = interfase to impl
		if (pair !in implsClasses) {
			implsClasses += pair
			impls.getOrPut(interfase) { arrayListOf() } += impl.newInstance()
		}
	}

	inline fun <reified T : Impl> load(): T = load(T::class.java)

	fun <T : Impl> load(clazz: Class<T>): T = loadList(clazz).firstOrNull() ?: invalidOp("Can't find implementation for ${clazz.name}")

	fun <T : Impl> loadList(clazz: Class<T>): List<T> {
		@Suppress("UNCHECKED_CAST")
		return (((impls[clazz] as? List<T>) ?: listOf()) + ServiceLoader.load(clazz))
			.filter { it.available }
			.sortedBy { it.priority }
			.toList()
	}
}