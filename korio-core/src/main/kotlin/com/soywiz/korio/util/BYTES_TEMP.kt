package com.soywiz.korio.util

import com.soywiz.korio.lang.threadLocal

val BYTES_EMPTY = byteArrayOf()
const val BYTES_TEMP_SIZE = 0x10000
val BYTES_TEMP by threadLocal { ByteArray(BYTES_TEMP_SIZE) }