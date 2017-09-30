package com.soywiz.korio.inject

import com.soywiz.korio.lang.KClass

expect object CreateAnnotation {
	fun <T> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T
}
