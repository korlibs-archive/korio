package com.soywiz.korio.vfs

import com.soywiz.korio.async.SuspendingSequence
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import java.io.Closeable

class LogVfs(val parent: VfsFile) : Vfs.Proxy() {
	val log = arrayListOf<String>()
	val logstr get() = log.toString()
	val modifiedFiles = LinkedHashSet<String>()
	suspend override fun access(path: String): VfsFile = parent[path]

	suspend override fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int {
		log += "exec($path, $cmdAndArgs, $env, $handler)"
		return super.exec(path, cmdAndArgs, env, handler)
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		log += "open($path, $mode)"
		val base = super.open(path, mode)
		return object : AsyncStreamBase() {
			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				base.position = position
				return base.read(buffer, offset, len)
			}

			suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				base.position = position
				base.write(buffer, offset, len)
				modifiedFiles += path
			}

			suspend override fun setLength(value: Long) {
				base.setLength(value)
				modifiedFiles += path
			}

			suspend override fun getLength(): Long {
				return base.getLength()
			}

			suspend override fun close() {
				return base.close()
			}
		}.toAsyncStream()
	}

	suspend override fun readRange(path: String, range: LongRange): ByteArray {
		log += "readRange($path, $range)"
		return super.readRange(path, range)
	}

	suspend override fun put(path: String, content: AsyncStream, attributes: List<Attribute>) {
		modifiedFiles += path
		log += "put($path, $content, $attributes)"
		super.put(path, content, attributes)
	}

	suspend override fun setSize(path: String, size: Long) {
		modifiedFiles += path
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
		modifiedFiles += path
		log += "delete($path)"
		return super.delete(path)
	}

	suspend override fun setAttributes(path: String, attributes: List<Attribute>) {
		modifiedFiles += path
		log += "setAttributes($path, $attributes)"
		super.setAttributes(path, attributes)
	}

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		modifiedFiles += path
		log += "mkdir($path, $attributes)"
		return super.mkdir(path, attributes)
	}

	suspend override fun touch(path: String, time: Long, atime: Long) {
		modifiedFiles += path
		log += "touch($path, $time, $atime)"
		super.touch(path, time, atime)
	}

	suspend override fun rename(src: String, dst: String): Boolean {
		modifiedFiles += src
		modifiedFiles += dst
		log += "rename($src, $dst)"
		return super.rename(src, dst)
	}

	suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
		log += "watch($path)"
		return super.watch(path, handler)
	}
}

fun VfsFile.log() = LogVfs(this).root
