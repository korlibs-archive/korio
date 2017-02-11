package com.soywiz.korio.vfs.jvm

import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.toAsync
import com.soywiz.korio.vfs.*
import java.io.File
import java.net.URLClassLoader

class ResourcesVfsProviderJvm : ResourcesVfsProvider() {
	override fun invoke(): Vfs {
		val classLoader: ClassLoader = ClassLoader.getSystemClassLoader()
		val merged = MergedVfs()

		return object : Vfs.Decorator(merged.root) {
			suspend override fun init() {
				if (classLoader is URLClassLoader) {
					for (url in classLoader.urLs) {
						val urlStr = url.toString()
						val vfs = if (urlStr.startsWith("http")) {
							UrlVfs(url)
						} else {
							LocalVfs(File(url.toURI()))
						}

						//println(vfs)

						if (vfs.extension in setOf("jar", "zip")) {
							//merged.vfsList += vfs.openAsZip()
						} else {
							merged.vfsList += vfs.jail()
						}
					}
					//println(merged.options)
				}

				//println("ResourcesVfsProviderJvm:classLoader:$classLoader")

				merged.vfsList += object : Vfs() {
					private fun normalize(path: String): String = path.trim('/')

					suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = executeInWorker {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:open: $path")
						MemorySyncStream(classLoader.getResourceAsStream(npath).readBytes()).toAsync()
					}

					suspend override fun stat(path: String): VfsStat = executeInWorker {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:stat: $npath")
						try {
							val s = classLoader.getResourceAsStream(npath)
							val size = s.available()
							s.read()
							createExistsStat(npath, isDirectory = false, size = size.toLong())
						} catch (e: Throwable) {
							//e.printStackTrace()
							createNonExistsStat(npath)
						}
					}
				}.root
			}

			override fun toString(): String = "ResourcesVfs"
		}
	}
}