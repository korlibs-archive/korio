package com.soywiz.coktvfs

suspend fun ZipVfs(file: VfsFile) = asyncFun {
    val s = file.open()
    s.setPosition(s.getLength() - 0x16)
    val data = s.readBytes(0x16).open()

    fun String.normalizeName() = this.trim('/')

    class ZipEntry(
            val name: String,
            val isDirectory: Boolean,
            val offset: Int,
            val compressedSize: Long,
            val uncompressedSize: Long
    ) {
    }

    fun ZipEntry?.toStat(file: VfsFile): VfsStat = VfsStat(file, this != null, this?.isDirectory ?: false, this?.uncompressedSize ?: 0L)

    val files = hashMapOf<String, ZipEntry>()

    data.apply {
        if (readS32_be() != 0x504B0506) throw IllegalStateException("Not a zip file")
        val diskNumber = readU16_le()
        val startDiskNumber = readU16_le()
        val entriesOnDisk = readU16_le()
        val entriesInDirectory = readU16_le()
        val directorySize = readS32_le()
        val directoryOffset = readS32_le()
        val commentLength = readU16_le()

        val ds = s.slice(directoryOffset.toLong(), directorySize.toLong()).readAvailable().open()
        ds.apply {
            for (n in 0 until entriesInDirectory) {
                if (readS32_be() != 0x504B0102) throw IllegalStateException("Not a zip file record")
                val versionMade = readU16_le()
                val versionExtract = readU16_le()
                val flags = readU16_le()
                val compressionMethod = readU16_le()
                val fileTime = readU16_le()
                val fileDate = readU16_le()
                val crc = readS32_le()
                val compressedSize = readS32_le()
                val uncompressedSize = readS32_le()
                val fileNameLength = readU16_le()
                val extraLength = readU16_le()
                val fileCommentLength = readU16_le()
                val diskNumberStart = readU16_le()
                val internalAttributes = readU16_le()
                val externalAttributes = readS32_le()
                val headerOffset = readS32_le()
                val name = readString(fileNameLength)
                val extra = readBytes(extraLength)

                val isDirectory = name.endsWith("/")
                val normalizedName = name.normalizeName()
                files[normalizedName] = ZipEntry(name, isDirectory, headerOffset, compressedSize.toUInt(), uncompressedSize.toUInt())
            }
        }
    }

    class Impl : Vfs() {
        suspend override fun stat(path: String): VfsStat {
            return files[path].toStat(file[path])
        }
    }

    Impl().root
}

suspend fun VfsFile.openAsZip() = ZipVfs(this)