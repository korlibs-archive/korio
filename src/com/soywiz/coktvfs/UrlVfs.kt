package com.soywiz.coktvfs

import java.net.URL

fun UrlVfs(url: URL): VfsFile {
    val urlStr = url.toString()

    class Impl : Vfs() {
        fun resolve(path: String) = URL("$urlStr/$path")

        suspend override fun readFully(path: String): ByteArray = asyncFun {
            val s = resolve(path).openStream()
            s.readBytes()
        }
    }
    return Impl().root
}