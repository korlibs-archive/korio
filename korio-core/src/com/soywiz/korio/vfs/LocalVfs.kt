package com.soywiz.korio.vfs

import com.soywiz.korio.service.Services
import java.io.File

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = LocalVfs(base)
	}
}

fun LocalVfs(base: String): VfsFile = VfsFile(localVfsProvider(), base)
fun TempVfs() = LocalVfs(System.getProperty("java.io.tmpdir"))
fun JailedLocalVfs(base: String): VfsFile = LocalVfs(base).jail()

fun CacheVfs() = LocalVfs(localVfsProvider.getCacheFolder()).jail()
fun ExternalStorageVfs() = LocalVfs(localVfsProvider.getExternalStorageFolder()).jail()

abstract class LocalVfsProvider : Services.Impl() {
	abstract operator fun invoke(): LocalVfs
	open fun getCacheFolder(): String = System.getProperty("java.io.tmpdir")
	open fun getExternalStorageFolder(): String = System.getProperty("java.io.tmpdir")
}
