package com.soywiz.korio.vfs

import com.soywiz.korio.util.compareToChain
import java.io.FileNotFoundException
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

suspend inline fun MountableVfs(callback: suspend Mountable.() -> Unit): VfsFile = suspendCoroutine { c ->
	val mount = object : Vfs.Proxy(), Mountable {
		private val mounts = TreeMap<String, VfsFile>({ a, b ->
			b.length.compareTo(a.length).compareToChain { b.compareTo(a) }
		})

		override fun mount(folder: String, file: VfsFile) = this.apply { mounts[VfsFile.normalize(folder)] = file }
		override fun unmount(folder: String): Mountable = this.apply { mounts.remove(VfsFile.normalize(folder)) }

		override suspend fun access(path: String): VfsFile {
			val rpath = VfsFile.normalize(path)
			for ((base, file) in mounts) {
				//println("$base/$file")
				if (rpath.startsWith(base)) {
					return file[rpath.substring(base.length)]
				}
			}
			throw FileNotFoundException(path)
		}
	}
	callback.startCoroutine(mount, object : Continuation<Unit> {
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

