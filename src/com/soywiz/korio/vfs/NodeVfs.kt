package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.stream.AsyncStream
import java.io.FileNotFoundException

open class NodeVfs : Vfs() {
	open class Node(
		val name: String,
		val isDirectory: Boolean = false,
		var parent: Node? = null
	) {
		init {
			parent?.children?.set(name, this)
		}

		var data: Any? = null
		val children = hashMapOf<String, Node>()
		val root: Node get() = parent?.root ?: this
		var stream: AsyncStream? = null

		fun child(name: String): Node? = when (name) {
			"", "." -> this
			".." -> parent
			else -> children[name]
		}

		fun createChild(name: String, isDirectory: Boolean = false): Node = Node(name, isDirectory = isDirectory, parent = this)

		operator fun get(path: String): Node = access(path, createFolders = false)

		fun access(path: String, createFolders: Boolean = false): Node {
			var node = if (path.startsWith('/')) root else this
			for (part in VfsUtil.parts(path)) {
				var child = node.child(part)
				if (child == null && createFolders) child = node.createChild(part, isDirectory = true)
				node = child ?: throw FileNotFoundException("Can't find '$part' in $path")
			}
			return node
		}

		fun mkdir(name: String) {
			createChild(name, isDirectory = true)
		}
	}

	val rootNode = Node("", isDirectory = true)

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val pathInfo = PathInfo(path)
		val folder = rootNode.access(pathInfo.folder)
		var node = folder.child(pathInfo.basename)
		if (node == null && mode.createIfNotExists) {
			node = folder.createChild(pathInfo.basename, isDirectory = false)
		}
		return node?.stream ?: throw FileNotFoundException(path)
	}

	suspend override fun stat(path: String): VfsStat = asyncFun {
		try {
			val node = rootNode.access(path)
			//createExistsStat(path, isDirectory = node.isDirectory, size = node.stream?.getLength() ?: 0L) // @TODO: Kotlin wrong code generated!
			val length = node.stream?.getLength() ?: 0L
			createExistsStat(path, isDirectory = node.isDirectory, size = length)
		} catch (e: Throwable) {
			createNonExistsStat(path)
		}
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
		val node = rootNode[path]
		for ((name, _) in node.children) {
			yield(file("$path/$name"))
		}
	}

	suspend override fun mkdir(path: String) {
		val pathInfo = PathInfo(path)
		rootNode.access(pathInfo.folder).mkdir(pathInfo.basename)
	}
}