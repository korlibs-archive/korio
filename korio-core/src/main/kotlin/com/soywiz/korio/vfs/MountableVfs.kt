@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs

import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.korioStartCoroutine
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import com.soywiz.korio.lang.FileNotFoundException

suspend fun MountableVfs(callback: suspend Mountable.() -> Unit): VfsFile = korioSuspendCoroutine { c ->
	val mount = object : Vfs.Proxy(), Mountable {
		private val mounts = ArrayList<Pair<String, VfsFile>>()

		override fun mount(folder: String, file: VfsFile) = this.apply {
			_unmount(folder)
			mounts += VfsUtil.normalize(folder) to file
			resort()
		}

		override fun unmount(folder: String): Mountable = this.apply {
			_unmount(folder)
			resort()
		}

		private fun _unmount(folder: String) {
			mounts.removeAll { it.first == VfsUtil.normalize(folder) }
		}

		private fun resort() {
			mounts.sortBy { it.first.length }
		}

		override suspend fun access(path: String): VfsFile {
			val rpath = VfsUtil.normalize(path)
			for ((base, file) in mounts) {
				//println("$base/$file")
				if (rpath.startsWith(base)) {
					return file[rpath.substring(base.length)]
				}
			}
			throw FileNotFoundException(path)
		}
	}
	callback.korioStartCoroutine(mount, object : Continuation<Unit> {
		override val context: CoroutineContext = c.context
		override fun resume(value: Unit) = c.resume(mount.root)
		override fun resumeWithException(exception: Throwable) = c.resumeWithException(exception)
	})
}

//inline fun MountableVfs(callback: Mountable.() -> Unit): VfsFile {
//	val mount = MountableVfs()
//	callback(mount)
//	return mount.root
//}

interface Mountable {
	fun mount(folder: String, file: VfsFile): Mountable
	fun unmount(folder: String): Mountable
}

