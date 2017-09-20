package com.soywiz.korio.vfs

import java.io.File

suspend fun ByteArray.writeToFile(file: File) = LocalVfs(file).write(this)
