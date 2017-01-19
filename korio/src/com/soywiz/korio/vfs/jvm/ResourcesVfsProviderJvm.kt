package com.soywiz.korio.vfs.jvm

import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.toAsync
import com.soywiz.korio.vfs.*
import java.io.File
import java.net.URLClassLoader

class ResourcesVfsProviderJvm : ResourcesVfsProvider {
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
							//merged.options += vfs.openAsZip()
						} else {
							merged.vfsList += vfs.jail()
						}
					}
					//println(merged.options)
				}

				merged.vfsList += object : Vfs() {
					suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = executeInWorker {
						MemorySyncStream(classLoader.getResourceAsStream(path).readBytes()).toAsync()
					}
				}.root
			}

			override fun toString(): String = "ResourcesVfs"
		}
	}
}