package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*

val tmpdir: String by lazy {
	when {
		isNodeJs -> require("os").tmpdir().unsafeCast<String>()
		else -> "/tmp"
	}
}

private val absoluteCwd: String by lazy { if (isNodeJs) require("path").resolve(".") else "." }

actual val ResourcesVfs: VfsFile by lazy { applicationVfs.jail() }
actual val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { jsLocalStorageVfs.root }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val tempVfs: VfsFile by lazy { localVfs(tmpdir) }

actual fun localVfs(path: String): VfsFile {
	return when {
		isNodeJs -> NodeJsLocalVfs()[path]
		else -> {
			//println("localVfs.url: href=$href, url=$url")
			UrlVfs(jsbaseUrl)[path]
		}
	}
}
