package com.soywiz.korio.vfs

import java.io.File

suspend fun ByteArray.writeToFile(path: String) = LocalVfs(path).write(this)
suspend fun ByteArray.writeToFile(file: File) = LocalVfs(file).write(this)
suspend fun ByteArray.writeToFile(file: VfsFile) = file.write(this)
