package com.soywiz.korio.serialization.json

fun Map<*, *>.toJson() = Json.encode(this)