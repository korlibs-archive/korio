package com.soywiz.korio.vfs

import java.net.URL

fun UrlVfs(url: String): VfsFile = urlVfsProvider()[url]
fun UrlVfs(url: URL): VfsFile = urlVfsProvider()[url.toString()]

interface UrlVfsProvider {
	operator fun invoke(): Vfs
}