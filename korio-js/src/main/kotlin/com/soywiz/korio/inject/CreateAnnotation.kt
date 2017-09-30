package com.soywiz.korio.inject

import com.soywiz.korio.lang.KClass

actual object CreateAnnotation {
	actual fun <T> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T = TODO()
}
