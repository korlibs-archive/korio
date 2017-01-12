package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.toAsync

suspend fun MemoryVfs(items: Map<String, AsyncStream> = mapOf()): VfsFile = asyncFun {
	val vfs = NodeVfs()
	for ((path, stream) in items) {
		val info = PathInfo(path)
		val folderNode = vfs.rootNode.access(info.folder, createFolders = true)
		val fileNode = folderNode.createChild(info.basename, isDirectory = false)
		fileNode.stream = stream
	}
	vfs.root
}

suspend fun MemoryVfsMix(items: Map<String, Any> = mapOf()): VfsFile = asyncFun {
	MemoryVfs(items.mapValues { (key, value) ->
		when (value) {
			is SyncStream -> value.toAsync()
			is ByteArray -> value.openAsync()
			is String -> value.toByteArray().openAsync()
			else -> value.toString().toByteArray().openAsync()
		}
	})
}

suspend fun MemoryVfsMix(vararg items: Pair<String, Any>): VfsFile = asyncFun {
	MemoryVfs(items.map { (key, value) ->
		key to when (value) {
			is SyncStream -> value.toAsync()
			is ByteArray -> value.openAsync()
			is String -> value.toByteArray().openAsync()
			else -> value.toString().toByteArray().openAsync()
		}
	}.toMap())
}
