package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
import java.net.*

fun UrlVfs(url: URL): VfsFile = UrlVfs(url.toString())
