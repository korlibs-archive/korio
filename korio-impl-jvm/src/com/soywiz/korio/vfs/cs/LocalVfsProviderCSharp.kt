package com.soywiz.korio.vfs.cs

import com.jtransc.annotation.JTranscAddMembers
import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.cs.CSharp
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.Promise
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.vfs.*
import java.io.Closeable

class LocalVfsProviderCSharp : LocalVfsProvider() {
	override fun invoke(): LocalVfs = CSharpVisualVfs()
}

@JTranscAddMembers(target = "cs", value = """
	System.IO.FileStream fs;
""")
class CSharpFileAsyncStream(val path: String, val mode: VfsOpenMode) : AsyncStreamBase() {
	init {
		_init(path)
	}

	private fun _init(path: String) = CSharp.v_raw("fs = System.IO.File.Open(N.istr(p0), System.IO.FileMode.Open, System.IO.FileAccess.Read, System.IO.FileShare.None);")

	private fun _setLength(p0: Long) = CSharp.v_raw("fs.Length = p0")
	private fun _getLength(): Long = CSharp.l_raw("fs.Length")
	private fun _close() = CSharp.v_raw("fs.Close();")

	private fun _read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		CSharp.v_raw("fs.Position = p0;")
		return CSharp.i_raw("fs.Read(p1.u(), p2, p3);")
	}

	private fun _write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		CSharp.v_raw("fs.Position = p0;")
		CSharp.v_raw("fs.Write(p1.u(), p2, p3);")
	}

	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = CSharp.runTaskAsync { _read(position, buffer, offset, len) }
	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = CSharp.runTaskAsync { _write(position, buffer, offset, len) }
	suspend override fun setLength(value: Long) = CSharp.runTaskAsync { _setLength(value) }
	suspend override fun getLength(): Long = CSharp.runTaskAsync { _getLength() }
	suspend override fun close() = CSharp.runTaskAsync { _close() }
}

class CSharpVisualVfs : LocalVfs() {
	override fun getAbsolutePath(path: String): String = path

	override val supportedAttributeTypes = listOf<Class<out Attribute>>()

	suspend override fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int {
		TODO()
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = CSharpFileAsyncStream(path, mode).toAsyncStream()

	suspend override fun setAttributes(path: String, attributes: List<Attribute>) {
		TODO()
	}

	//private fun getFileInfo(path: String) = CSharp.raw<Any>("N.wrap(new System.IO.FileInfo(N.istr(p0)))")
	//private fun fileInfoExists(data: Any) = CSharp.b_raw("((System.IO.FileInfo)N.unwrap(p0)).Exists")
	//private fun fileInfoSize(data: Any) = CSharp.l_raw("((System.IO.FileInfo)N.unwrap(p0)).Length")
	//private fun fileInfoIsDirectory(data: Any) = CSharp.b_raw("((System.IO.FileInfo)N.unwrap(p0)).Length")

	private fun fileSize(path: String) = CSharp.l_raw("(new System.IO.FileInfo(N.istr(p0))).Length")
	private fun fileExists(path: String) = CSharp.b_raw("System.IO.File.Exists(N.istr(p0))")
	private fun directoryExists(path: String) = CSharp.b_raw("System.IO.Directory.Exists(N.istr(p0))")

	suspend override fun stat(path: String): VfsStat {
		return CSharp.runTaskAsync {
			val fileExists = fileExists(path)
			val directoryExists = directoryExists(path)
			if (fileExists) {
				createExistsStat(path, isDirectory = false, size = fileSize(path))
			} else if (directoryExists) {
				createExistsStat(path, isDirectory = true, size = 0L)
			} else {
				createNonExistsStat(path)
			}
		}
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> {
		TODO()
	}

	@JTranscMethodBody(target = "cs", value = """
		var path = N.istr(p0);
		try {
			var res = !System.IO.Directory.Exists(path);
			System.IO.Directory.CreateDirectory(path);
			return res;
		} catch (Exception) {
			return false;
		}
	""")
	external private fun createDirectory(p0: String): Boolean

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		return CSharp.runTaskAsync { createDirectory(path) }
	}

	suspend override fun delete(path: String): Boolean {
		TODO()
	}

	suspend override fun rename(src: String, dst: String): Boolean {
		TODO()
	}

	suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
		TODO()
	}
}

suspend fun <T> CSharp.runTaskAsync(task: () -> T): T {
	val deferred = Promise.Deferred<T>()

	CSharp.runTask {
		deferred.resolve(task())
	}

	return deferred.promise.await()
}
