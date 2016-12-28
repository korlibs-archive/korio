package com.soywiz.korio.vfs

import com.soywiz.korio.stream.AsyncStream

suspend fun MemoryVfs(items: Map<String, AsyncStream> = mapOf()): VfsFile = TODO()