package com.soywiz.korio.vfs

import com.soywiz.korio.async.SuspendingSequence
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.error.ignoreErrors

open class MergedVfs(vfsList: List<VfsFile> = listOf()) : Vfs.Proxy() {
	val vfsList = ArrayList(vfsList)

	suspend override fun access(path: String): VfsFile {
		if (vfsList.size == 1) {
			return vfsList.first()[path]
		} else {
			return vfsList.map { it[path] }.firstOrNull { it.exists() } ?: vfsList.first()[path]
		}
	}

	suspend override fun stat(path: String): VfsStat {
		for (vfs in vfsList) {
			val result = ignoreErrors { vfs[path].stat() } ?: continue
			if (result.exists) return result.copy(file = file(path))
		}
		return createNonExistsStat(path)
	}

	suspend override fun list(path: String): SuspendingSequence<VfsFile> = asyncGenerate {
		val emitted = LinkedHashSet<String>()
		for (vfs in vfsList) {
			val items = ignoreErrors { vfs[path].list() } ?: continue

			try {
				for (v in items) {
					if (v.basename !in emitted) {
						emitted += v.basename
						yield(file("$path/${v.basename}"))
					}
				}
			} catch (e: Throwable) {

			}
		}
	}
}
