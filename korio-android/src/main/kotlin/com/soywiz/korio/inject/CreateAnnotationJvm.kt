package com.soywiz.korio.inject

import java.lang.reflect.Proxy
import kotlin.reflect.KClass

actual object CreateAnnotation {
	actual fun <T : Any> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T {
		val kclass = (clazz as kotlin.reflect.KClass<Any>)
		return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), arrayOf(kclass.java)) { proxy, method, args ->
			map[method.name]
		} as T
	}
}
