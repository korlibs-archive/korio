package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.*

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = LocalVfs(base)
	}

	override fun toString(): String = "LocalVfs"
}

val resourcesVfs: VfsFile by lazy { ResourcesVfs }
val rootLocalVfs: VfsFile by lazy { KorioNative.rootLocalVfs() }
val applicationVfs: VfsFile by lazy { KorioNative.applicationVfs() }
val cacheVfs: VfsFile by lazy { KorioNative.cacheVfs() }
val externalStorageVfs: VfsFile by lazy { KorioNative.externalStorageVfs() }
val userHomeVfs: VfsFile by lazy { KorioNative.userHomeVfs() }
val localCurrentDirVfs: VfsFile by lazy { ApplicationVfs() }
val tempVfs: VfsFile by lazy { KorioNative.tempVfs() }

// Deprecated
fun RootLocalVfs(): VfsFile = rootLocalVfs

fun ApplicationVfs(): VfsFile = applicationVfs
fun CacheVfs(): VfsFile = cacheVfs
fun ExternalStorageVfs(): VfsFile = externalStorageVfs
fun UserHomeVfs(): VfsFile = userHomeVfs
fun LocalCurrentDirVfs(): VfsFile = localCurrentDirVfs
fun TempVfs(): VfsFile = tempVfs

fun LocalVfs(base: String): VfsFile = KorioNative.localVfs(base)
fun JailedLocalVfs(base: String): VfsFile = LocalVfs(base).jail()
