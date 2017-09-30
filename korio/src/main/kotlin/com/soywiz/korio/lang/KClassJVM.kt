package com.soywiz.korio.lang

//actual typealias KClass<T> = kotlin.reflect.KClass<T>

actual inline fun <reified T> classOf(): KClass<T> = T::class as KClass<T>
