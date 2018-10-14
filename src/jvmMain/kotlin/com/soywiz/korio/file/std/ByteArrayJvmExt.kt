package com.soywiz.korio.file.std

import java.io.*

suspend fun ByteArray.writeToFile(file: File) = LocalVfs(file).write(this)
