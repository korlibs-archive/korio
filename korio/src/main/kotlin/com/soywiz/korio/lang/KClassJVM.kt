package com.soywiz.korio.lang

//impl typealias KClass<T> = kotlin.reflect.KClass<T>

impl inline fun <reified T> classOf(): KClass<T> = T::class as KClass<T>
