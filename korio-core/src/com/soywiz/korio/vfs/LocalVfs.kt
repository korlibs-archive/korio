package com.soywiz.korio.vfs

import java.io.File
import java.util.*

fun LocalVfs(base: String): VfsFile = LocalVfs()[base]
fun TempVfs() = LocalVfs()[System.getProperty("java.io.tmpdir")]
fun LocalVfs(base: File): VfsFile = LocalVfs()[base.absolutePath]
fun JailedLocalVfs(base: File): VfsFile = LocalVfs()[base.absolutePath].jail()
fun JailedLocalVfs(base: String): VfsFile = LocalVfs()[base].jail()
suspend fun File.open(mode: VfsOpenMode) = LocalVfs(this).open(mode)

fun LocalVfs(): VfsFile = localVfsProvider().root

interface LocalVfsProvider {
	operator fun invoke(): Vfs
}