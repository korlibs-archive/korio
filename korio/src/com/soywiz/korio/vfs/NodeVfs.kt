package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.slice
import com.soywiz.korio.stream.toAsync
import java.io.FileNotFoundException

open class NodeVfs : Vfs() {
	open class Node(
		val name: String,
		val isDirectory: Boolean = false,
		parent: Node? = null
	) : Iterable<Node> {
		override fun iterator(): Iterator<Node> = children.values.iterator()

		var parent: Node? = null
			get() = field
			set(value) {
				if (field != null) {
					field!!.children.remove(this.name)
				}
				field = value
				field?.children?.set(name, this)
			}

		init {
			this.parent = parent
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

		fun mkdir(name: String): Boolean {
			if (child(name) != null) {
				return false
			} else {
				createChild(name, isDirectory = true)
				return true
			}
		}
	}

	val rootNode = Node("", isDirectory = true)

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = asyncFun {
		val pathInfo = PathInfo(path)
		val folder = rootNode.access(pathInfo.folder)
		var node = folder.child(pathInfo.basename)
		if (node == null && mode.createIfNotExists) {
			node = folder.createChild(pathInfo.basename, isDirectory = false)
			node.stream = MemorySyncStream().toAsync()
		}
		node?.stream?.clone() ?: throw FileNotFoundException(path)
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

	suspend override fun delete(path: String): Boolean {
		return super.delete(path)
	}

	suspend override fun mkdir(path: String): Boolean {
		val pathInfo = PathInfo(path)
		return rootNode.access(pathInfo.folder).mkdir(pathInfo.basename)
	}

	suspend override fun rename(src: String, dst: String): Boolean {
		if (src == dst) return false
		val dstInfo = PathInfo(dst)
		val srcNode = rootNode.access(src)
		val dstFolder = rootNode.access(dstInfo.folder)
		srcNode.parent = dstFolder
		return true
	}
}