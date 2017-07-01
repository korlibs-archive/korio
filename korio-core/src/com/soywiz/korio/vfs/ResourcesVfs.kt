package com.soywiz.korio.vfs

import com.soywiz.korio.service.Services

val ResourcesVfs by lazy { resourcesVfsProvider().root }

abstract class ResourcesVfsProvider : Services.Impl() {
	abstract operator fun invoke(classLoader: ClassLoader): Vfs
	operator fun invoke(): Vfs = invoke(ClassLoader.getSystemClassLoader())
	operator fun invoke(clazz: Class<*>): Vfs = invoke(clazz.classLoader)
}