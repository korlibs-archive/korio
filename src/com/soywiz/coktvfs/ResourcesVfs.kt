package com.soywiz.coktvfs

import com.soywiz.coktvfs.async.asyncFun
import com.soywiz.coktvfs.async.executeInWorker
import com.soywiz.coktvfs.stream.AsyncStream
import com.soywiz.coktvfs.stream.open
import com.soywiz.coktvfs.stream.toAsync

fun ResourcesVfs(classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): VfsFile {
    class Impl : Vfs() {
        suspend override fun open(path: String): AsyncStream = asyncFun {
            readFully(path).open().toAsync()
        }

        suspend override fun readFully(path: String): ByteArray = executeInWorker {
            classLoader.getResourceAsStream(path).readBytes()
        }

        override fun toString(): String = "ResourcesVfs"
    }
    return Impl().root
}