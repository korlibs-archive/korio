@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs.android

import com.soywiz.korio.android.KorioAndroidContext
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.coroutine.withCoroutineContext
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.toAsyncInWorker
import com.soywiz.korio.vfs.*
import java.io.FileNotFoundException

val AndroidAssetsVfs by lazy {
	object : Vfs() {
		val ctx = KorioAndroidContext
		val resources = ctx.resources
		val assets = resources.assets

		suspend override fun list(path: String): AsyncSequence<VfsFile> = withCoroutineContext {
			asyncGenerate(this@withCoroutineContext) {
				val path2 = path.trim('/')
				for (name in assets.list(path2)) {
					yield(file("$path2/$name".trim('/')))
				}
			}
		}

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			return MemorySyncStream(assets.open(path.trim('/')).readBytes()).toAsyncInWorker()
		}

		suspend override fun stat(path: String): VfsStat {
			val path2 = path.trim('/')
			return try {
				val size = assets.open(path2).use { f ->
					f.available()
				}
				createExistsStat(path2, isDirectory = false, size = size.toLong())
			} catch (e: FileNotFoundException) {
				val list = try {
					assets.list(path2)
				} catch (e: Throwable) {
					null
				}
				//println("$path2: ${list?.toList()}")
				val isDirectory = (list != null) && list.isNotEmpty()
				if (isDirectory) {
					createExistsStat(path2, isDirectory = true, size = 2048)
				} else {
					//e.printStackTrace()
					createNonExistsStat(path2)
				}
			}
		}

		override fun toString(): String = "AssetsVfs"
	}.root
}

class ResourcesVfsProviderAndroid : ResourcesVfsProvider() {
	val merged = object : MergedVfs() {
		override fun toString(): String = "ResourcesVfs"
	}

	override fun invoke(): Vfs = object : Vfs.Decorator(merged.root) {
		suspend override fun init() {
			val ai = KorioAndroidContext.applicationInfo
			val source = ai.sourceDir
			merged.vfsList += LocalVfs(source).openAsZip()
			merged.vfsList += AndroidAssetsVfs
		}
	}
}