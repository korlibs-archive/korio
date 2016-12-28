package com.soywiz.korio.util

val BYTES_TEMP_TL by lazy { ThreadLocal.withInitial { ByteArray(0x10000) } }
val BYTES_TEMP: ByteArray get() = BYTES_TEMP_TL.get()