package com.soywiz.korio.vfs

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = LocalVfs(base)
	}
}

header val localVfsProvider: LocalVfsProvider
header val tmpdir: String

fun LocalVfs(base: String): VfsFile = VfsFile(localVfsProvider(), base)
fun TempVfs() = LocalVfs(tmpdir)
fun JailedLocalVfs(base: String): VfsFile = LocalVfs(base).jail()

fun CacheVfs() = LocalVfs(localVfsProvider.getCacheFolder()).jail()
fun ExternalStorageVfs() = LocalVfs(localVfsProvider.getExternalStorageFolder()).jail()
fun UserHomeVfs() = LocalVfs(localVfsProvider.getExternalStorageFolder()).jail()

abstract class LocalVfsProvider {
	abstract operator fun invoke(): LocalVfs
	open fun getCacheFolder(): String = tmpdir
	open fun getExternalStorageFolder(): String = tmpdir
}

