package com.soywiz.korio.serialization.yaml

import com.soywiz.korio.serialization.*
import kotlin.reflect.*

inline fun <reified T : Any> Yaml.decodeToType(s: String, mapper: ObjectMapper): T =
	decodeToType(s, T::class, mapper)

@Suppress("UNCHECKED_CAST")
fun <T : Any> Yaml.decodeToType(s: String, clazz: KClass<T>, mapper: ObjectMapper): T =
	mapper.toTyped(clazz, Yaml.decode(s))

