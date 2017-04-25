package com.soywiz.korio.vfs

import com.soywiz.korio.async.SuspendingSequence
import com.soywiz.korio.stream.AsyncStream
import java.io.Closeable

class LogVfs(val parent: VfsFile) : Vfs.Proxy() {
	val log = arrayListOf<String>()
	val logstr get() = log.toString()
	suspend override fun access(path: String): VfsFile = parent[path]

	suspend override fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int {
		log += "exec($path, $cmdAndArgs, $env, $handler)"
		return super.exec(path, cmdAndArgs, env, handler)
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		log += "open($path, $mode)"
		return super.open(path, mode)
	}

	suspend override fun readRange(path: String, range: LongRange): ByteArray {
		log += "readRange($path, $range)"
		return super.readRange(path, range)
	}

	suspend override fun put(path: String, content: AsyncStream, attributes: List<Attribute>) {
		log += "put($path, $content, $attributes)"
		super.put(path, content, attributes)
	}

	suspend override fun setSize(path: String, size: Long) {
		log += "setSize($path, $size)"
		super.setSize(path, size)
	}

	suspend override fun stat(path: String): VfsStat {
		log += "stat($path)"
		return super.stat(path)
	}

	suspend override fun list(path: String): SuspendingSequence<VfsFile> {
		log += "list($path)"
		return super.list(path)
	}

	suspend override fun delete(path: String): Boolean {
		log += "delete($path)"
		return super.delete(path)
	}

	suspend override fun setAttributes(path: String, attributes: List<Attribute>) {
		log += "setAttributes($path, $attributes)"
		super.setAttributes(path, attributes)
	}

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		log += "mkdir($path, $attributes)"
		return super.mkdir(path, attributes)
	}

	suspend override fun touch(path: String, time: Long, atime: Long) {
		log += "touch($path, $time, $atime)"
		super.touch(path, time, atime)
	}

	suspend override fun rename(src: String, dst: String): Boolean {
		log += "rename($src, $dst)"
		return super.rename(src, dst)
	}

	suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
		log += "watch($path)"
		return super.watch(path, handler)
	}
}

fun VfsFile.log() = LogVfs(this).root
