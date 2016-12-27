package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.coroutines.suspendCoroutine

fun LocalVfs(base: File): VfsFile {
    val baseAbsolutePath = base.absolutePath

    class Impl : Vfs() {
        fun resolve(path: String) = "$baseAbsolutePath/$path"

        suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
            val channel = AsynchronousFileChannel.open(Paths.get(path), when (mode) {
                VfsOpenMode.READ -> StandardOpenOption.READ
                VfsOpenMode.WRITE -> StandardOpenOption.WRITE
                VfsOpenMode.APPEND -> StandardOpenOption.APPEND
                VfsOpenMode.CREATE -> StandardOpenOption.CREATE
                VfsOpenMode.CREATE_NEW -> StandardOpenOption.CREATE_NEW
                VfsOpenMode.TRUNCATE_EXISTING -> StandardOpenOption.TRUNCATE_EXISTING
            })

            return object : AsyncStream() {
                var position = 0L

                suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
                    val bb = ByteBuffer.wrap(buffer, offset, len)
                    val read = completionHandler<Int> { channel.read(bb, position, Unit, it) }
                    position += read
                    read
                }

                suspend override fun write(buffer: ByteArray, offset: Int, len: Int) = asyncFun {
                    val bb = ByteBuffer.wrap(buffer, offset, len)
                    val write = completionHandler<Int> { channel.write(bb, position, Unit, it) }
                    position += write
                    Unit
                }

                suspend override fun setPosition(value: Long) = run { position = value }
                suspend override fun getPosition(): Long = position
                suspend override fun setLength(value: Long) {
                    channel.truncate(value)
                }

                suspend override fun getLength(): Long = channel.size()
            }
        }

        suspend override fun setSize(path: String, size: Long): Unit = executeInWorker {
            val file = File(resolve(path))
            FileOutputStream(file, true).channel.use { outChan ->
                outChan.truncate(size)
            }
        }

        suspend override fun stat(path: String): VfsStat = executeInWorker {
            val file = File(resolve(path))
            VfsStat(
                    file = VfsFile(this@Impl, "$path/${file.name}"),
                    exists = file.exists(),
                    isDirectory = file.isDirectory,
                    size = file.length()
            )
        }

        suspend override fun list(path: String) = executeInWorker {
            asyncGenerate {
                for (path in Files.newDirectoryStream(Paths.get(resolve(path)))) {
                    val file = path.toFile()
                    yield(VfsStat(
                            file = VfsFile(this@Impl, file.absolutePath.substring(baseAbsolutePath.length + 1)),
                            exists = file.exists(),
                            isDirectory = file.isDirectory,
                            size = file.length()
                    ))
                }
            }
        }

        override fun toString(): String = "LocalVfs(${base.absolutePath})"
    }
    return Impl().root
}

inline suspend fun <T> completionHandler(crossinline callback: (CompletionHandler<T, Unit>) -> Unit) = suspendCoroutine<T> { c ->
    callback(object : CompletionHandler<T, Unit> {
        override fun completed(result: T, attachment: Unit?) {
            c.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Unit?) {
            c.resumeWithException(exc)
        }
    })
}
