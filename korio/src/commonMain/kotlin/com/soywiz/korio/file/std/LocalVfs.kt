package com.soywiz.korio.file.std

import com.soywiz.korio.file.*

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = localVfs(base)
	}

	override fun toString(): String = "LocalVfs"
}

var resourcesVfsDebug = false
expect val resourcesVfs: VfsFile
expect val rootLocalVfs: VfsFile
expect val applicationVfs: VfsFile
expect val applicationDataVfs: VfsFile
expect val cacheVfs: VfsFile
expect val externalStorageVfs: VfsFile
expect val userHomeVfs: VfsFile
expect val tempVfs: VfsFile
val localCurrentDirVfs: VfsFile get() = applicationVfs

expect fun localVfs(path: String): VfsFile
fun jailedLocalVfs(base: String): VfsFile = localVfs(base).jail()
