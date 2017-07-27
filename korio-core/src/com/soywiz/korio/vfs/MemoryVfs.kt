@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs

import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.toAsync

suspend fun MemoryVfs(items: Map<String, AsyncStream> = mapOf(), caseSensitive: Boolean = true): VfsFile {
	val vfs = NodeVfs(caseSensitive)
	for ((path, stream) in items) {
		val info = PathInfo(path)
		val folderNode = vfs.rootNode.access(info.folder, createFolders = true)
		val fileNode = folderNode.createChild(info.basename, isDirectory = false)
		fileNode.stream = stream
	}
	return vfs.root
}

suspend fun MemoryVfsMix(items: Map<String, Any> = mapOf(), caseSensitive: Boolean = true): VfsFile = MemoryVfs(items.mapValues { (_, v) ->
	when (v) {
		is SyncStream -> v.toAsync()
		is ByteArray -> v.openAsync()
		is String -> v.toByteArray().openAsync()
		else -> v.toString().toByteArray().openAsync()
	}
}, caseSensitive)

suspend fun MemoryVfsMix(vararg items: Pair<String, Any>, caseSensitive: Boolean = true): VfsFile = MemoryVfs(items.map { (key, value) ->
	key to when (value) {
		is SyncStream -> value.toAsync()
		is ByteArray -> value.openAsync()
		is String -> value.toByteArray().openAsync()
		else -> value.toString().toByteArray().openAsync()
	}
}.toMap(), caseSensitive)
