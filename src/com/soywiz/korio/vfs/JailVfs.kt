package com.soywiz.korio.vfs

fun JailVfs(jailRoot: VfsFile): VfsFile = object : Vfs.Proxy() {
    override suspend fun access(path: String): VfsFile = jailRoot[VfsUtil.normalize(path)]
}.root

