@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*
import kotlin.coroutines.*

suspend fun MountableVfs(callback: suspend Mountable.() -> Unit): VfsFile =
	suspendCoroutine { c ->
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
				mounts.sortBy { -it.first.length }
			}

			override suspend fun access(path: String): VfsFile {
				val rpath = VfsUtil.normalize(path)
				for ((base, file) in mounts) {
					//println("$base/$file")
					if (rpath.startsWith(base)) return file[rpath.substring(base.length)]
				}
				throw FileNotFoundException(path)
			}

			override suspend fun getUnderlyingUnscapedFile(path: String) = access(path).getUnderlyingUnscapedFile()

			override fun toString(): String = "MountableVfs"
		}
		callback.startCoroutine(mount, object : Continuation<Unit> {
			override val context: CoroutineContext = c.context

			override fun resumeWith(result: Result<Unit>) {
				val exception = result.exceptionOrNull()
				if (exception != null) {
					c.resumeWithException(exception)
				} else {
					c.resume(mount.root)
				}
			}
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

