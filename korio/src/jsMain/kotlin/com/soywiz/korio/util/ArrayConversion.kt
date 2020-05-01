package com.soywiz.korio.util

import org.khronos.webgl.*

fun ArrayBuffer.toByteArray(): ByteArray = Int8Array(this).asByteArray()
fun Int8Array.asByteArray(): ByteArray = this.unsafeCast<ByteArray>()
