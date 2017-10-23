package com.soywiz.korio.vfs

import com.soywiz.korio.KorioNative

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = LocalVfs(base)
	}
}


val localVfsProvider get() = KorioNative.localVfsProvider
val tmpdir: String get() = KorioNative.tmpdir

fun LocalVfs(base: String): VfsFile = localVfsProvider(base)
fun TempVfs() = LocalVfs(tmpdir)
fun JailedLocalVfs(base: String): VfsFile = LocalVfs(base).jail()

fun CacheVfs() = LocalVfs(localVfsProvider.getCacheFolder()).jail()
fun ExternalStorageVfs() = LocalVfs(localVfsProvider.getExternalStorageFolder()).jail()
fun UserHomeVfs() = LocalVfs(localVfsProvider.getExternalStorageFolder()).jail()

abstract class LocalVfsProvider {
	abstract operator fun invoke(): LocalVfs
	operator open fun invoke(path: String): VfsFile = VfsFile(this(), path)
	open fun getCacheFolder(): String = tmpdir
	open fun getExternalStorageFolder(): String = tmpdir
}

