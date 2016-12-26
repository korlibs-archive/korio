package com.soywiz.coktvfs

import com.soywiz.coktvfs.async.AsyncSequence
import com.soywiz.coktvfs.async.asyncFun
import com.soywiz.coktvfs.async.asyncGenerate
import com.soywiz.coktvfs.stream.AsyncStream
import java.nio.charset.Charset
import java.util.*

class VfsFile(
        val vfs: Vfs,
        path: String
) {
    val path: String = normalize(path)
    val basename: String by lazy { path.substringAfterLast('/') }

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

    suspend fun open(): AsyncStream = vfs.open(path)

    suspend fun read(): ByteArray = vfs.readFully(path)
    suspend fun write(data: ByteArray): Unit = vfs.writeFully(path, data)

    suspend fun readString(charset: Charset = Charsets.UTF_8): String = asyncFun { vfs.readFully(path).toString(charset) }
    suspend fun writeString(data: String, charset: Charset = Charsets.UTF_8): Unit = asyncFun { vfs.writeFully(path, data.toByteArray(charset)) }

    suspend fun readChunk(offset: Long, size: Int): ByteArray = vfs.readChunk(path, offset, size)
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

    fun jail(): VfsFile = JailVfs(this)

    suspend fun list(): AsyncSequence<VfsStat> = vfs.list(path)

    suspend fun listRecursive(): AsyncSequence<VfsStat> = asyncGenerate {
        for (file in list()) {
            yield(file)
            if (file.isDirectory) {
                for (file in file.file.listRecursive()) {
                    yield(file)
                }
            }
        }
    }

    override fun toString(): String = "$vfs[$path]"
}