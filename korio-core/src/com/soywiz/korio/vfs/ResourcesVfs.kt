package com.soywiz.korio.vfs

import com.soywiz.korio.service.Services

val ResourcesVfs: VfsFile by lazy { resourcesVfsProvider().root }

abstract class ResourcesVfsProvider : Services.Impl() {
	abstract operator fun invoke(): Vfs
}