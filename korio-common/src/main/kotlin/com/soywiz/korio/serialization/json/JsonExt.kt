package com.soywiz.korio.serialization.json

import com.soywiz.korio.serialization.ObjectMapper

fun Map<*, *>.toJson(mapper: ObjectMapper) = Json.encode(this, mapper)