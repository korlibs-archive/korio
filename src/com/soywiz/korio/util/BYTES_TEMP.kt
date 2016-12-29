package com.soywiz.korio.util

val BYTES_TEMP by threadLocal { ByteArray(0x10000) }