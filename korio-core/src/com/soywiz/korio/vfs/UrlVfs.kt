package com.soywiz.korio.vfs

import java.net.URL
import java.util.*

fun UrlVfs(url: String): VfsFile = urlVfsProvider()[url]
fun UrlVfs(url: URL): VfsFile = urlVfsProvider()[url.toString()]

private val urlVfsProvider by lazy {
	ServiceLoader.load(UrlVfsProvider::class.java).firstOrNull()
		?: throw UnsupportedOperationException("UrlVfsProvider not defined")
}

interface UrlVfsProvider {
	operator fun invoke(): Vfs
}