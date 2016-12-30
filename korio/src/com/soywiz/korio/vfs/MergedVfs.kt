package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate

class MergedVfs(options: List<VfsFile> = listOf()) : Vfs.Proxy() {
	val options = ArrayList(options)

	suspend override fun access(path: String): VfsFile = asyncFun {
		options.map { it[path] }.firstOrNull { it.exists() } ?: file(path)
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
		val emitted = HashSet<String>()
		for (option in options) {
			for (v in option[path].list()) {
				if (v.basename !in emitted) {
					emitted += v.basename
					yield(file("$path/${v.basename}"))
				}
			}
		}
	}
}
