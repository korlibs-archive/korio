package com.soywiz.coktvfs

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

fun LocalVfs(base: File): VfsFile {
    val baseAbsolutePath = base.absolutePath

    class Impl : Vfs() {
        fun resolve(path: String) = "$baseAbsolutePath/$path"

        suspend override fun readChunk(path: String, offset: Long, size: Long) = super.readChunk(path, offset, size)
        suspend override fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean) = super.writeChunk(path, data, offset, resize)
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

