package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.toAsync

fun ResourcesVfs(classLoader: ClassLoader = ClassLoader.getSystemClassLoader()): VfsFile {
	class Impl : Vfs() {
		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = asyncFun {
			readFully(path).openSync().toAsync()
		}

		suspend override fun readFully(path: String): ByteArray = executeInWorker {
			classLoader.getResourceAsStream(path).readBytes()
		}

		override fun toString(): String = "ResourcesVfs"
	}
	return Impl().root
}