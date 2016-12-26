package com.soywiz.coktvfs

fun ResourcesVfs(classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): VfsFile {
    class Impl : Vfs() {
        suspend override fun open(path: String): AsyncStream = asyncFun {
            readFully(path).open().toAsync()
        }

        suspend override fun readFully(path: String): ByteArray = executeInWorker {
            classLoader.getResourceAsStream(path).readBytes()
        }
    }
    return Impl().root
}