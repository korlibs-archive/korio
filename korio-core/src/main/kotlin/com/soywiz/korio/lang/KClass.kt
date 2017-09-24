package com.soywiz.korio.lang

// @TODO: Hack since we don't have access to KClass from common
typealias KClass<T> = Any
//header interface KClass<T>

//header inline fun <reified T> classOf(): KClass<T> = T::class as KClass<T>
header inline fun <reified T> classOf(): KClass<T>

//fun <T> classOf(instance: T): KClass<T> = TODO()