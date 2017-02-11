package com.soywiz.korio.vfs

import com.soywiz.korio.service.Services

val localVfsProvider: LocalVfsProvider by lazy { Services.load<LocalVfsProvider>() }
val resourcesVfsProvider: ResourcesVfsProvider by lazy { Services.load<ResourcesVfsProvider>() }
