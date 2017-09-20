package com.soywiz.korio.vfs

suspend fun ByteArray.writeToFile(path: String) = LocalVfs(path).write(this)
suspend fun ByteArray.writeToFile(file: VfsFile) = file.write(this)
