package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream

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