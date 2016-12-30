package com.soywiz.korio.vfs

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.js.BrowserJsUtils
import com.soywiz.korio.vfs.js.NodeJsUtils
import java.io.File
import java.net.URLClassLoader

val ResourcesVfs by lazy { ResourcesVfs(ClassLoader.getSystemClassLoader() as URLClassLoader) }

fun ResourcesVfs(classLoader: URLClassLoader): VfsFile = ResourcesVfsGen(classLoader).root

@JTranscMethodBody(target = "js", value = "return {% SMETHOD com.soywiz.korio.vfs.ResourcesVfsGenJs:gen %}();")
private fun ResourcesVfsGen(classLoader: URLClassLoader): Vfs {
	return ResourcesVfsJvm(classLoader)
}

@Suppress("unused")
object ResourcesVfsGenJs {
	@JvmStatic fun gen(): Vfs {
		return EmbededResourceListing(if (OS.isNodejs) JailedLocalVfs(NodeJsUtils.getCWD()) else UrlVfs(BrowserJsUtils.getBaseUrl()))
	}
}

@Suppress("unused")
private class EmbededResourceListing(parent: VfsFile) : Vfs.Decorator(parent) {
	val nodeVfs = NodeVfs()

	init {
		_init()
	}

	@JTranscMethodBody(target = "js", value = """
		{% for asset in assetFiles %}
		this['{% METHOD #CLASS:_addFile %}'](N.str({{ asset.path|quote }}), {{ asset.size }});
		{% end %}
	""")
	fun _init() {
	}

	@Suppress("unused")
	private fun _addFile(fullpath: String, size: Double) {
		val info = PathInfo(fullpath)
		val folder = nodeVfs.rootNode.access(info.folder, createFolders = true)
		folder.createChild(info.basename, isDirectory = false).data = size.toLong()
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

private class ResourcesVfsJvm(val classLoader: URLClassLoader, private val merged: MergedVfs = MergedVfs()) : Vfs.Decorator(merged.root) {
	suspend override fun init() = asyncFun {
		for (url in classLoader.urLs) {
			val urlStr = url.toString()
			val vfs = if (urlStr.startsWith("http")) {
				UrlVfs(url)
			} else {
				LocalVfs(File(url.toURI()))
			}

			if (vfs.extension in setOf("jar", "zip")) {
				//merged.options += vfs.openAsZip()
			} else {
				merged.options += vfs.jail()
			}
		}
		//println(merged.options)
	}

	override fun toString(): String = "ResourcesVfs"
}