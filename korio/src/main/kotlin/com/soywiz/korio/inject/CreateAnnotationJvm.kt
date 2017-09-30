package com.soywiz.korio.inject

import kotlin.reflect.KClass
import java.lang.reflect.Proxy

actual object CreateAnnotation {
	actual fun <T : Any> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T {
		val kclass = (clazz as kotlin.reflect.KClass<Any>)
		return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), arrayOf(kclass.java)) { proxy, method, args ->
			map[method.name]
		} as T
	}
}
