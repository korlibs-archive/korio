package com.soywiz.korio.nio

import java.nio.ByteBuffer
import java.nio.charset.Charset

fun ByteBuffer.toString(charset: Charset = Charsets.UTF_8) = charset.decode(this)