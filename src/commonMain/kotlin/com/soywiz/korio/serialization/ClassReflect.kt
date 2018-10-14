package com.soywiz.korio.serialization

import kotlin.reflect.*

class AsyncFun<T : Any>(val name: String, val call: suspend T.(args: List<Any?>) -> Any?)

class ClassReflect<T : Any>(
	val clazz: KClass<T>,
	val props: List<KProperty1<T, *>> = listOf(),
	val propTypes: List<KClass<*>> = listOf(),
	val smethods: List<AsyncFun<T>> = listOf(),
	val gen: Gen.() -> T = { TODO() }
) {
	object Gen {
		fun <T> get(index: Int): T = TODO()
	}
}

fun ObjectMapper.register(cr: ClassReflect<*>): ObjectMapper {
	TODO()
}

suspend fun asyncCaptureStdout(callback: suspend () -> Unit): String {
	TODO()
}