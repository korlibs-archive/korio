package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = LocalVfs(base)
	}

	override fun toString(): String = "LocalVfs"
}

expect val ResourcesVfs: VfsFile
expect val rootLocalVfs: VfsFile
expect val applicationVfs: VfsFile
expect val applicationDataVfs: VfsFile
expect val cacheVfs: VfsFile
expect val externalStorageVfs: VfsFile
expect val userHomeVfs: VfsFile
expect val tempVfs: VfsFile

expect fun localVfs(path: String): VfsFile

val localCurrentDirVfs: VfsFile by lazy { ApplicationVfs() }

// Deprecated
fun RootLocalVfs(): VfsFile = rootLocalVfs

fun ApplicationVfs(): VfsFile = applicationVfs
fun CacheVfs(): VfsFile = cacheVfs
fun ExternalStorageVfs(): VfsFile = externalStorageVfs
fun UserHomeVfs(): VfsFile = userHomeVfs
fun LocalCurrentDirVfs(): VfsFile = localCurrentDirVfs
fun TempVfs(): VfsFile = tempVfs

fun LocalVfs(base: String): VfsFile = localVfs(base)
fun JailedLocalVfs(base: String): VfsFile = LocalVfs(base).jail()
