package com.soywiz.korio.lang

import com.soywiz.korio.*
import kotlin.reflect.*

val <T : Any> KClass<T>.portableSimpleName: String get() = KorioNative.getClassSimpleName(this)
