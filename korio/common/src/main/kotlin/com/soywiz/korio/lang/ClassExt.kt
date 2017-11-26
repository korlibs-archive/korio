package com.soywiz.korio.lang

import com.soywiz.korio.KorioNative
import kotlin.reflect.KClass

val <T : Any> KClass<T>.portableSimpleName: String get() = KorioNative.getClassSimpleName(this)
