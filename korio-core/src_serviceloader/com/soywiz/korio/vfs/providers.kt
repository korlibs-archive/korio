package com.soywiz.korio.vfs

import java.util.*

val localVfsProvider: LocalVfsProvider by lazy {
	ServiceLoader.load(LocalVfsProvider::class.java).firstOrNull()
			?: throw UnsupportedOperationException("LocalVfsProvider not defined")
}

val resourcesVfsProvider: ResourcesVfsProvider by lazy {
	ServiceLoader.load(ResourcesVfsProvider::class.java).firstOrNull()
			?: throw UnsupportedOperationException("ResourcesVfsProvider not defined")
}
