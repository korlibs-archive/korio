package com.soywiz.korio.inject

import kotlin.reflect.KClass

expect object CreateAnnotation {
	fun <T : Any> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T
}
