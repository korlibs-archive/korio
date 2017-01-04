package com.soywiz.korio.vfs

val ResourcesVfs by lazy { resourcesVfsProvider().root }

interface ResourcesVfsProvider {
	operator fun invoke(): Vfs
}