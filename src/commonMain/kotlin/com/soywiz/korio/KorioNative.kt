package com.soywiz.korio

import com.soywiz.korio.file.*

expect object KorioNative {
	val ResourcesVfs: VfsFile
	fun rootLocalVfs(): VfsFile
	fun applicationVfs(): VfsFile
	fun applicationDataVfs(): VfsFile
	fun cacheVfs(): VfsFile
	fun externalStorageVfs(): VfsFile
	fun userHomeVfs(): VfsFile
	fun localVfs(path: String): VfsFile
	fun tempVfs(): VfsFile
}

