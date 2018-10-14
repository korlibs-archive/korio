@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

fun MemoryVfs(items: Map<String, AsyncStream> = lmapOf(), caseSensitive: Boolean = true): VfsFile {
	val vfs = NodeVfs(caseSensitive)
	for ((path, stream) in items) {
		val info = PathInfo(path)
		val folderNode = vfs.rootNode.access(info.folder, createFolders = true)
		val fileNode = folderNode.createChild(info.basename, isDirectory = false)
		fileNode.stream = stream
	}
	return vfs.root
}

fun MemoryVfsMix(
	items: Map<String, Any> = lmapOf(),
	caseSensitive: Boolean = true,
	charset: Charset = UTF8
): VfsFile = MemoryVfs(items.mapValues { (_, v) ->
	when (v) {
		is SyncStream -> v.toAsync()
		is ByteArray -> v.openAsync()
		is String -> v.openAsync(charset)
		else -> v.toString().toByteArray(charset).openAsync()
	}
}, caseSensitive)

fun MemoryVfsMix(
	vararg items: Pair<String, Any>,
	caseSensitive: Boolean = true,
	charset: Charset = UTF8
): VfsFile = MemoryVfs(items.map { (key, value) ->
	key to when (value) {
		is SyncStream -> value.toAsync()
		is ByteArray -> value.openAsync()
		is String -> value.openAsync(charset)
		else -> value.toString().toByteArray(charset).openAsync()
	}
}.toMap(), caseSensitive)
