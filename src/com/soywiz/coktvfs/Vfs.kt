package com.soywiz.coktvfs

abstract class Vfs {
    val root by lazy { VfsFile(this, "/") }

    suspend open fun readFully(path: String) = asyncFun {
        val stat = stat(path)
        readChunk(path, 0L, stat.size)
    }

    suspend open fun writeFully(path: String, data: ByteArray) = writeChunk(path, data, 0L, true)

    suspend open fun readChunk(path: String, offset: Long, size: Long): ByteArray = throw UnsupportedOperationException()
    suspend open fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean): Unit = throw UnsupportedOperationException()
    suspend open fun setSize(path: String, size: Long): Unit = throw UnsupportedOperationException()
    suspend open fun stat(path: String): VfsStat = throw UnsupportedOperationException()
    suspend open fun list(path: String): AsyncSequence<VfsStat> = throw UnsupportedOperationException()

    abstract class Proxy : Vfs() {
        abstract protected fun access(path: String): VfsFile
        open protected fun transformStat(stat: VfsStat): VfsStat = stat

        suspend override fun readFully(path: String): ByteArray = access(path).read()
        suspend override fun writeFully(path: String, data: ByteArray): Unit = access(path).write(data)
        suspend override fun readChunk(path: String, offset: Long, size: Long): ByteArray = access(path).readChunk(offset, size)
        suspend override fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean): Unit = access(path).writeChunk(data, offset, resize)
        suspend override fun setSize(path: String, size: Long): Unit = access(path).setSize(size)
        suspend override fun stat(path: String): VfsStat = asyncFun { transformStat(access(path).stat()) }
        suspend override fun list(path: String) = asyncGenerate {
            for (it in access(path).list()) {
                yield(transformStat(it))
            }
        }
    }
}