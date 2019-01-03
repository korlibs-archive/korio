package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
import kotlinx.coroutines.*
import java.io.*
import java.security.*

private val secureRandom: SecureRandom by lazy { SecureRandom.getInstanceStrong() }

private val absoluteCwd = File(".").absolutePath

actual val ResourcesVfs: VfsFile by lazy { ResourcesVfsProviderJvm()().root.jail() }
actual val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val tempVfs: VfsFile by lazy { localVfs(tmpdir) }

actual fun localVfs(path: String): VfsFile = LocalVfsJvm()[path]


val tmpdir: String get() = System.getProperty("java.io.tmpdir")
