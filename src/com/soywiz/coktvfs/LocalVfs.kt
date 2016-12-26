package com.soywiz.coktvfs

import com.soywiz.coktvfs.async.asyncGenerate
import com.soywiz.coktvfs.async.executeInWorker
import com.soywiz.coktvfs.stream.AsyncStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Paths

fun LocalVfs(base: File): VfsFile {
    val baseAbsolutePath = base.absolutePath

    class Impl : Vfs() {
        fun resolve(path: String) = "$baseAbsolutePath/$path"

        suspend override fun open(path: String): AsyncStream {
            val raf = RandomAccessFile(File(resolve(path)), "r")
            return object : AsyncStream() {
                suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker { raf.read(buffer, offset, len) }
                suspend override fun write(buffer: ByteArray, offset: Int, len: Int) = executeInWorker { raf.write(buffer, offset, len) }
                suspend override fun setPosition(value: Long) = executeInWorker { raf.seek(value) }
                suspend override fun getPosition(): Long = executeInWorker { raf.filePointer }
                suspend override fun setLength(value: Long) = executeInWorker { raf.setLength(value) }
                suspend override fun getLength(): Long = executeInWorker { raf.length() }
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

