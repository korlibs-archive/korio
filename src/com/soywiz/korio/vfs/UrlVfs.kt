package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.util.JsMethodBody
import java.net.URL

fun UrlVfs(url: URL): VfsFile {
    val urlStr = url.toString()

    class Impl : Vfs() {
        fun resolve(path: String) = URL("$urlStr/$path")

        @JsMethodBody("""
            var path = N.istr(p0);
            var continuation = p1;
            throw "Not implemented!";
        """)
        suspend override fun readFully(path: String): ByteArray = asyncFun {
            val s = resolve(path).openStream()
            s.readBytes()
        }
    }
    return Impl().root
}