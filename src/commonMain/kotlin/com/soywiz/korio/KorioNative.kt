package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.math.*

expect object KorioNative {
	val ResourcesVfs: VfsFile

	val websockets: WebSocketClientFactory

	fun rootLocalVfs(): VfsFile
	fun applicationVfs(): VfsFile
	fun applicationDataVfs(): VfsFile
	fun cacheVfs(): VfsFile
	fun externalStorageVfs(): VfsFile
	fun userHomeVfs(): VfsFile
	fun localVfs(path: String): VfsFile
	fun tempVfs(): VfsFile

	fun Thread_sleep(time: Long): Unit

	fun enterDebugger()

	fun getenv(key: String): String?
}

