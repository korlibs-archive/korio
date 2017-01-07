package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import java.io.FileNotFoundException

open class MergedVfs(vfsList: List<VfsFile> = listOf()) : Vfs.Proxy() {
	val vfsList = ArrayList(vfsList)

	suspend override fun access(path: String): VfsFile = asyncFun {
		vfsList.map { it[path] }.firstOrNull { it.exists() } ?: throw FileNotFoundException(path)
	}

	suspend override fun stat(path: String): VfsStat = asyncFun {
		for (vfs in vfsList) {
			val result = try {
				vfs[path].stat()
			} catch (t: Throwable) {
				null
			}
			if (result != null && result.exists) return@asyncFun result!!.copy(file = file(path))
		}
		createNonExistsStat(path)
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
		val emitted = LinkedHashSet<String>()
		for (vfs in vfsList) {
			val items = try {
				vfs[path].list()
			} catch (e: UnsupportedOperationException) {
				continue
			}
			for (v in items) {
				if (v.basename !in emitted) {
					emitted += v.basename
					yield(file("$path/${v.basename}"))
				}
			}
		}
	}
}
