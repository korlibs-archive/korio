package com.soywiz.korio.lang

import com.soywiz.korio.*
import kotlin.reflect.*

expect val <T : Any> KClass<T>.portableSimpleName: String
