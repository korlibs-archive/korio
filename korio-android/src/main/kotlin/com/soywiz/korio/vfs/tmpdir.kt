package com.soywiz.korio.vfs

actual val tmpdir: String get() = System.getProperty("java.io.tmpdir")