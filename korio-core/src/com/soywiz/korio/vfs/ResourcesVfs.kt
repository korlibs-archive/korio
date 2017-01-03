package com.soywiz.korio.vfs

import java.net.URLClassLoader
import java.util.*

val ResourcesVfs by lazy { ResourcesVfs(ClassLoader.getSystemClassLoader() as URLClassLoader) }

fun ResourcesVfs(classLoader: URLClassLoader): VfsFile = resourcesVfsProvider(classLoader).root

private val resourcesVfsProvider by lazy {
	ServiceLoader.load(ResourcesVfsProvider::class.java).firstOrNull()
		?: throw UnsupportedOperationException("ResourcesVfsProvider not defined")
}

interface ResourcesVfsProvider {
	operator fun invoke(classLoader: URLClassLoader): Vfs
}