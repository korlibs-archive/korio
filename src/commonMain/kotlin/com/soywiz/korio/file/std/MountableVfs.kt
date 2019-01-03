@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*

suspend fun MountableVfs(callback: suspend Mountable.() -> Unit): VfsFile {
	val mount = object : Vfs.Proxy(), Mountable {
		private val mounts = ArrayList<Pair<String, VfsFile>>()

		override fun mount(folder: String, file: VfsFile) = this.apply {
			unmountInternal(folder)
			mounts += folder.pathInfo.normalize() to file
			resort()
		}

		override fun unmount(folder: String): Mountable = this.apply {
			unmountInternal(folder)
			resort()
		}

		private fun unmountInternal(folder: String) {
			mounts.removeAll { it.first == folder.pathInfo.normalize() }
		}

		private fun resort() {
			mounts.sortByDescending { it.first.length }
		}

		override suspend fun access(path: String): VfsFile {
			val rpath = path.pathInfo.normalize()
			for ((base, file) in mounts) {
				//println("$base/$file")
				if (rpath.startsWith(base)) return file[rpath.substring(base.length)]
			}
			throw FileNotFoundException(path)
		}

		override suspend fun getUnderlyingUnscapedFile(path: String) = access(path).getUnderlyingUnscapedFile()

		override fun toString(): String = "MountableVfs"
	}

	callback(mount)

	return mount.root
}

interface Mountable {
	fun mount(folder: String, file: VfsFile): Mountable
	fun unmount(folder: String): Mountable
}

