package com.soywiz.coktvfs

import java.nio.charset.Charset
import java.util.*

// @TODO: Give feedback about this!
// @NOTE: Would be great if I could extend VfsFile inside the AsyncStreamController/Awaitable interface
// so I could provide here a 'stat' method (and the other methods) as an alias for statAsync().await() as an extension instead
// of having to provide it in the controller.

// Examples:
//suspend fun Awaitable.stat(file: VfsFile, c: Continuation<VfsStat>) { // Modifier suspend is not aplicable to top level function
//	file.statAsync().then(resolved = { c.resume(it) }, rejected = { c.resumeWithException(it) })
//}
//
//suspend fun Awaitable::VfsFile.stat(c: Continuation<VfsStat>) {
//	this.statAsync().then(resolved = { c.resume(it) }, rejected = { c.resumeWithException(it) })
//}

class VfsFile(
        val vfs: Vfs,
        path: String
) {
    val path: String = normalize(path)

    companion object {
        fun normalize(path: String): String {
            var path2 = path
            while (path2.startsWith("/")) path2 = path2.substring(1)
            val out = LinkedList<String>()
            for (part in path2.split("/")) {
                when (part) {
                    "", "." -> Unit
                    ".." -> if (out.isNotEmpty()) out.removeLast()
                    else -> out += part
                }
            }
            return out.joinToString("/")
        }

        fun combine(base: String, access: String): String = normalize(base + "/" + access)
    }

    operator fun get(path: String): VfsFile = VfsFile(vfs, combine(this.path, path))

    suspend fun read(): ByteArray = vfs.readFully(path)
    suspend fun write(data: ByteArray): Unit = vfs.writeFully(path, data)

    suspend fun readString(charset: Charset = Charsets.UTF_8): String = asyncFun { vfs.readFully(path).toString(charset) }
    suspend fun writeString(data: String, charset: Charset = Charsets.UTF_8): Unit = asyncFun { vfs.writeFully(path, data.toByteArray(charset)) }

    suspend fun readChunk(offset: Long, size: Long): ByteArray = vfs.readChunk(path, offset, size)
    suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit = vfs.writeChunk(path, data, offset, resize)

    suspend fun stat(): VfsStat = vfs.stat(path)
    suspend fun size(): Long = asyncFun { vfs.stat(path).size }
    suspend fun exists(): Boolean = asyncFun {
        try {
            vfs.stat(path).exists
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun setSize(size: Long): Unit = vfs.setSize(path, size)

    fun jail(): VfsFile = JailVfs(this).root

    suspend fun list(): AsyncSequence<VfsStat> = vfs.list(path)

    //fun listRecursive(): AsyncStream<VfsStat> = generateAsync {
    // @TODO: Report ERROR: java.lang.IllegalAccessError: tried to access field com.soywiz.coktvfs.VfsFile$listRecursive$1.controller from class com.soywiz.coktvfs.VfsFile$listRecursive$1$1
    // @TODO: This was a runtime error, if not supported this should be a compile-time error
    //
    //	this@VfsFile.list().eachAsync {
    //		emit(it)
    //	}
    //}

    suspend fun listRecursive(): AsyncSequence<VfsStat> = asyncGenerate {
        // @TODO: This is not lazy at all! (at least per directory). Find a way to flatMap lazily this
        for (file in list()) {
            yield(file)
            if (file.isDirectory) {
                for (file in file.file.listRecursive()) {
                    yield(file)
                }
            }
        }
    }

    override fun toString(): String = "VfsFile($vfs, $path)"
}