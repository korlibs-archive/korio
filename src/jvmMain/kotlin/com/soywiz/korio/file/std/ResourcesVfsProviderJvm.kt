package com.soywiz.korio.file.std

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*
import java.io.*
import java.net.*

class ResourcesVfsProviderJvm {
	operator fun invoke(): Vfs = invoke(ClassLoader.getSystemClassLoader())

	operator fun invoke(classLoader: ClassLoader): Vfs {
		val merged = MergedVfs()

		return object : Vfs.Decorator(merged.root) {
			override suspend fun init() {
				//println("localCurrentDirVfs: $localCurrentDirVfs, ${localCurrentDirVfs.absolutePath}")

				// @TODO: IntelliJ doesn't properly set resources folder for MPP just yet (on gradle works just fine),
				// @TODO: so at least we try to load resources from sources until this is fixed.
				for (folder in listOf(
					localCurrentDirVfs["src/commonMain/resources"],
					localCurrentDirVfs["src/jvmMain/resources"],
					localCurrentDirVfs["resources"],
					localCurrentDirVfs["jvmResources"]
				)) {
					if (folder.exists() && folder.isDirectory()) {
						merged += folder.jail()
					}
				}


				if (classLoader is URLClassLoader) {
					for (url in classLoader.urLs) {
						//println("ResourcesVfsProviderJvm.url: $url")
						val urlStr = url.toString()
						val vfs = when {
							urlStr.startsWith("http") -> UrlVfs(url)
							else -> LocalVfs(File(url.toURI()))
						}

						//println(vfs)

						when {
							vfs.extension in setOf("jar", "zip") -> {
								//merged.vfsList += vfs.openAsZip()
							}
							else -> merged += vfs.jail()
						}
					}
					//println(merged.options)
				} else {
					//println("ResourcesVfsProviderJvm.classLoader not URLClassLoader: $classLoader")
				}

				//println("ResourcesVfsProviderJvm:classLoader:$classLoader")

				merged += object : Vfs() {
					private fun normalize(path: String): String = path.trim('/')

					private fun getResourceAsStream(npath: String) = classLoader.getResourceAsStream(npath)
						?: classLoader.getResourceAsStream("/$npath")
						?: invalidOp("Can't find '$npath' in ResourcesVfsProviderJvm")

					override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:open: $path")
						return MemorySyncStream(getResourceAsStream(npath).readBytes()).toAsync()
					}

					override suspend fun stat(path: String): VfsStat = run {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:stat: $npath")
						try {
							val s = getResourceAsStream(npath)
							val size = s.available()
							s.read()
							createExistsStat(npath, isDirectory = false, size = size.toLong())
						} catch (e: Throwable) {
							//e.printStackTrace()
							createNonExistsStat(npath)
						}
					}

					override fun toString(): String = "ResourcesVfsProviderJvm"
				}.root

				println("ResourcesVfsProviderJvm: $merged")
			}

			override fun toString(): String = "ResourcesVfs"
		}
	}
}