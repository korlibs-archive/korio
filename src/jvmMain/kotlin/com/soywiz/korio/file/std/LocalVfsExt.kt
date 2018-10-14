package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
import java.io.*

operator fun LocalVfs.Companion.get(base: File) = LocalVfs(base)
fun LocalVfs(base: File): VfsFile = LocalVfs(base.absolutePath)
fun JailedLocalVfs(base: File): VfsFile = LocalVfs(base.absolutePath).jail()
suspend fun File.open(mode: VfsOpenMode) = LocalVfs(this).open(mode)
fun File.toVfs() = LocalVfs(this)
