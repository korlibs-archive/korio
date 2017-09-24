package com.soywiz.korio.inject

import com.soywiz.korio.lang.KClass
import java.lang.reflect.Proxy

impl object CreateAnnotation {
	impl fun <T> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T {
		val kclass = (clazz as kotlin.reflect.KClass<Any>)
		return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), arrayOf(kclass.java)) { proxy, method, args ->
			map[method.name]
		} as T
	}
}
