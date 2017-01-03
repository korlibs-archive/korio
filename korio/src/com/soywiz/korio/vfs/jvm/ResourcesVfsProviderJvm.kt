package com.soywiz.korio.vfs.jvm

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.vfs.*
import java.io.File
import java.net.URLClassLoader

class ResourcesVfsProviderJvm : ResourcesVfsProvider {
	val merged = MergedVfs()
	override fun invoke(classLoader: URLClassLoader): Vfs = object : Vfs.Decorator(merged.root) {
		suspend override fun init() = asyncFun {
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
					merged.options += vfs.jail()
				}
			}
			//println(merged.options)
		}

		override fun toString(): String = "ResourcesVfs"
	}
}
