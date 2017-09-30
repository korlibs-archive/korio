package com.soywiz.korio.inject

import kotlin.reflect.KClass

actual object CreateAnnotation {
	actual fun <T : Any> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T = TODO()
}
