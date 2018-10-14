package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*

val ResourcesVfs: VfsFile get() = KorioNative.ResourcesVfs
val ApplicationDataVfs: VfsFile get() = KorioNative.applicationDataVfs()
