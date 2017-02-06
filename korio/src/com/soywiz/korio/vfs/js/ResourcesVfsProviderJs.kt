package com.soywiz.korio.vfs.js

import com.jtransc.js.*
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.*

class ResourcesVfsProviderJs : ResourcesVfsProvider {
	override fun invoke(): Vfs {
		return EmbededResourceListing(if (OS.isNodejs) {
			LocalVfs(getCWD())
		} else {
			UrlVfs(getBaseUrl())
		}.jail())
	}

	private fun getCWD(): String = global["process"].call("cwd").toJavaString()

	private fun getBaseUrl(): String {
		var baseHref = document["location"]["href"].call("replace", jsRegExp("/[^\\/]*$"), "")
		val bases = document.call("getElementsByTagName", "base")
		if (bases["length"].toInt() > 0) baseHref = bases[0]["href"]
		return baseHref.toJavaString()
	}
}

@Suppress("unused")
private class EmbededResourceListing(parent: VfsFile) : Vfs.Decorator(parent) {
	val nodeVfs = NodeVfs()

	init {
		for (asset in jsGetAssetStats()) {
			val info = PathInfo(asset.path.trim('/'))
			val folder = nodeVfs.rootNode.access(info.folder, createFolders = true)
			folder.createChild(info.basename, isDirectory = false).data = asset.size
		}
	}

	suspend override fun stat(path: String): VfsStat {
		try {
			val n = nodeVfs.rootNode[path]
			return createExistsStat(path, n.isDirectory, n.data as Long)
		} catch (t: Throwable) {
			return createNonExistsStat(path)
		}
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
		for (item in nodeVfs.rootNode[path]) {
			yield(file("$path/${item.name}"))
		}
	}

	override fun toString(): String = "ResourcesVfs[$parent]"
}
