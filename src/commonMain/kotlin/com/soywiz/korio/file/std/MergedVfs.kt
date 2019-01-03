package com.soywiz.korio.file.std

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*

open class MergedVfs(vfsList: List<VfsFile> = listOf()) : Vfs.Proxy() {
	private val vfsList = ArrayList(vfsList)

	operator fun plusAssign(other: VfsFile) {
		vfsList += other
	}

	operator fun minusAssign(other: VfsFile) {
		vfsList -= other
	}

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
			val items = runIgnoringExceptions { vfs[path].list() } ?: continue

			try {
				for (v in items) {
					if (v.baseName !in emitted) {
						emitted += v.baseName
						yield(file("$path/${v.baseName}"))
					}
				}
			} catch (e: Throwable) {

			}
		}
	}

	override fun toString(): String = "MergedVfs($vfsList)"
}
