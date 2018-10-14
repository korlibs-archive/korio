package com.soywiz.korio.file.std

import com.soywiz.korio.async.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*

open class MergedVfs(vfsList: List<VfsFile> = listOf()) : Vfs.Proxy() {
	val vfsList = ArrayList(vfsList)

	override suspend fun access(path: String): VfsFile {
		if (vfsList.size == 1) {
			return vfsList.first()[path]
		} else {
			return vfsList.map { it[path] }.firstOrNull { it.exists() } ?: vfsList.first()[path]
		}
	}

	override suspend fun stat(path: String): VfsStat {
		for (vfs in vfsList) {
			val result = vfs[path].stat()
			if (result.exists) return result.copy(file = file(path))
		}
		return createNonExistsStat(path)
	}

	override suspend fun list(path: String): SuspendingSequence<VfsFile> = asyncGenerate {
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

	override fun toString(): String = "MergedVfs"
}
