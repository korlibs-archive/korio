@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs

import com.soywiz.korio.coroutine.*
import com.soywiz.korio.util.compareToChain
import java.io.FileNotFoundException
import java.util.*

suspend fun MountableVfs(callback: suspend Mountable.() -> Unit): VfsFile = korioSuspendCoroutine { c ->
	val mount = object : Vfs.Proxy(), Mountable {
		private val mounts = TreeMap<String, VfsFile>({ a, b ->
			b.length.compareTo(a.length).compareToChain { b.compareTo(a) }
		})

		override fun mount(folder: String, file: VfsFile) = this.apply { mounts[VfsUtil.normalize(folder)] = file }
		override fun unmount(folder: String): Mountable = this.apply { mounts.remove(VfsUtil.normalize(folder)) }

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
		override val context: CoroutineContext = EmptyCoroutineContext
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

