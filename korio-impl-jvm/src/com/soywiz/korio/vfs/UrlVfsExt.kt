package com.soywiz.korio.vfs

import java.net.URL

fun UrlVfs(url: URL): VfsFile = UrlVfs(url.toString(), Unit).root
