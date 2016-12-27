package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.stream.*

suspend fun IsoVfs(file: VfsFile): VfsFile = asyncFun {
    ISO.openVfs(file.open(VfsOpenMode.READ))
}

suspend fun VfsFile.openAsIso() = IsoVfs(this)

object ISO {
    const val SECTOR_SIZE = 0x800L

    suspend fun read(s: AsyncStream): IsoFile = IsoReader(s).read()

    suspend fun openVfs(s: AsyncStream) = asyncFun {
        val iso = read(s)
        (object : Vfs() {
            val isoFile = iso

            fun getVfsStat(file: IsoFile): VfsStat = VfsStat(VfsFile(this, file.fullname), exists = true, isDirectory = file.isDirectory, size = file.size)

            suspend override fun stat(path: String): VfsStat = asyncFun {
                try {
                    getVfsStat(isoFile[path])
                } catch (e: Throwable) {
                    createNonExistsStat(path)
                }
            }

            suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
                return isoFile[path].open2(mode)
            }

            suspend override fun list(path: String) = asyncGenerate {
                val file = isoFile[path]
                for (c in file.children) {
                    yield(getVfsStat(c))
                }
            }
        }).root
    }

    class IsoReader(val s: AsyncStream) {
        fun getSector(sector: Int, size: Int): AsyncStream = s.sliceWithSize(sector.toLong() * SECTOR_SIZE, size.toLong())
        suspend fun getSectorMemory(sector: Int, size: Int) = asyncFun { getSector(sector, size).readAvailable().open() }

        suspend fun read(): IsoFile = asyncFun {
            val primary = PrimaryVolumeDescriptor(getSectorMemory(0x10, SECTOR_SIZE.toInt()))
            val root = IsoFile(this@IsoReader, primary.rootDirectoryRecord, null)
            readDirectoryRecords(root, getSectorMemory(primary.rootDirectoryRecord.extent, primary.rootDirectoryRecord.size))
            root
        }

        suspend fun readDirectoryRecords(parent: IsoFile, sector: SyncStream): Unit = asyncFun {
            while (!sector.eof) {
                val dr = DirectoryRecord(sector)
                if (dr.length == 0) {
                    // @TODO: Do without while!
                    while ((sector.position % SECTOR_SIZE) != 0L) {
                        sector.position++
                    }
                    continue
                }
                if (dr.name == "" || dr.name == "\u0001") continue
                val file = IsoFile(this@IsoReader, dr, parent)

                if (dr.isDirectory) readDirectoryRecords(file, getSectorMemory(dr.extent, dr.size))
            }
        }
    }

    class IsoFile(val reader: IsoReader, val record: DirectoryRecord, val parent: IsoFile?) {
        val name: String get() = record.name
        val isDirectory: Boolean get() = record.isDirectory
        val fullname: String = if (parent == null) record.name else "${parent.fullname}/${record.name}".trimStart('/')
        val children = arrayListOf<IsoFile>()
        val size: Long = record.size.toLong()

        init {
            parent?.children?.add(this)
        }

        fun dump() {
            println("$fullname: $record")
            for (c in children) c.dump()
        }

        fun open2(mode: VfsOpenMode) = reader.getSector(record.extent, record.size)
        operator fun get(name: String): IsoFile {
            var current = this
            for (part in name.split("/")) {
                when (part) {
                    "" -> Unit
                    "." -> Unit
                    ".." -> current = current.parent!!
                    else -> current = current.children.firstOrNull { it.name.toUpperCase() == part.toUpperCase() } ?: throw IllegalStateException("Can't find part $part for accessing path $name children: ${current.children}")
                }
            }
            return current
        }

        override fun toString(): String {
            return "IsoFile(fullname='$fullname', size=$size)"
        }
    }

    fun SyncStream.readLongArray_le(count: Int): LongArray = (0 until count).map { readS64_le() }.toLongArray()

    fun SyncStream.readU32_le_be(): Int {
        val le = readS32_le()
        readS32_be()
        return le
    }

    fun SyncStream.readTextWithLength(): String {
        val len = readU8()
        return readStringz(len)
    }

    fun SyncStream.readU16_le_be(): Int {
        val le = readS16_le()
        readS16_be()
        return le
    }

    data class PrimaryVolumeDescriptor(
            val volumeDescriptorHeader: VolumeDescriptorHeader,
            val pad1: Int,
            val systemId: String,
            val volumeId: String,
            val pad2: Long,
            val volumeSpaceSize: Int,
            val pad3: LongArray,
            val volumeSetSize: Int,
            val volumeSequenceNumber: Int,
            val logicalBlockSize: Int,
            val pathTableSize: Int,
            val typeLPathTable: Int,
            val optType1PathTable: Int,
            val typeMPathTable: Int,
            val optTypeMPathTable: Int,
            val rootDirectoryRecord: DirectoryRecord,
            val volumeSetId: String,
            val publisherId: String,
            val preparerId: String,
            val applicationId: String,
            val copyrightFileId: String,
            val abstractFileId: String,
            val bibliographicFileId: String,
            val creationDate: IsoDate,
            val modificationDate: IsoDate,
            val expirationDate: IsoDate,
            val effectiveDate: IsoDate,
            val fileStructureVersion: Int,
            val pad5: Int,
            val applicationData: ByteArray,
            val pad6: ByteArray
            //fixed byte Pad6_[653];
    ) {
        constructor(s: SyncStream) : this(
                volumeDescriptorHeader = VolumeDescriptorHeader(s),
                pad1 = s.readU8(),
                systemId = s.readStringz(0x20),
                volumeId = s.readStringz(0x20),
                pad2 = s.readS64_le(),
                volumeSpaceSize = s.readU32_le_be(),
                pad3 = s.readLongArray_le(4),
                volumeSetSize = s.readU16_le_be(),
                volumeSequenceNumber = s.readU16_le_be(),
                logicalBlockSize = s.readU16_le_be(),
                pathTableSize = s.readU32_le_be(),
                typeLPathTable = s.readS32_le(),
                optType1PathTable = s.readS32_le(),
                typeMPathTable = s.readS32_le(),
                optTypeMPathTable = s.readS32_le(),
                rootDirectoryRecord = DirectoryRecord(s),
                volumeSetId = s.readStringz(0x80),
                publisherId = s.readStringz(0x80),
                preparerId = s.readStringz(0x80),
                applicationId = s.readStringz(0x80),
                copyrightFileId = s.readStringz(37),
                abstractFileId = s.readStringz(37),
                bibliographicFileId = s.readStringz(37),
                creationDate = IsoDate(s),
                modificationDate = IsoDate(s),
                expirationDate = IsoDate(s),
                effectiveDate = IsoDate(s),
                fileStructureVersion = s.readU8(),
                pad5 = s.readU8(),
                applicationData = s.readBytes(0x200),
                pad6 = s.readBytes(653)
        )
    }

    data class VolumeDescriptorHeader(
            val type: TypeEnum,
            val id: String,
            val version: Int
    ) {
        enum class TypeEnum(val id: Int) {
            BootRecord(0x00),
            VolumePartitionSetTerminator(0xFF),
            PrimaryVolumeDescriptor(0x01),
            SupplementaryVolumeDescriptor(0x02),
            VolumePartitionDescriptor(0x03);

            companion object {
                val BY_ID = values().associateBy { it.id }
            }
        }

        constructor(s: SyncStream) : this(
                type = TypeEnum.BY_ID[s.readU8()]!!,
                id = s.readStringz(5),
                version = s.readU8()
        )
    }

    data class IsoDate(val data: String) {
        constructor(s: SyncStream) : this(data = s.readString(17))

        val year = data.substring(0, 4).toIntOrNull() ?: 0
        val month = data.substring(4, 6).toIntOrNull() ?: 0
        val day = data.substring(6, 8).toIntOrNull() ?: 0
        val hour = data.substring(8, 10).toIntOrNull() ?: 0
        val minute = data.substring(10, 12).toIntOrNull() ?: 0
        val second = data.substring(12, 14).toIntOrNull() ?: 0
        val hsecond = data.substring(14, 16).toIntOrNull() ?: 0
        //val offset = data.substring(16).toInt()

        override fun toString(): String = "IsoDate(%04d-%02d-%02d %02d:%02d:%02d.%d)".format(year, month, day, hour, minute, second, hsecond)
    }

    data class DateStruct(
            val year: Int,
            val month: Int,
            val day: Int,
            val hour: Int,
            val minute: Int,
            val second: Int,
            val offset: Int
    ) {
        constructor(s: SyncStream) : this(
                year = s.readU8(),
                month = s.readU8(),
                day = s.readU8(),
                hour = s.readU8(),
                minute = s.readU8(),
                second = s.readU8(),
                offset = s.readU8()
        )

        val fullYear = 1900 + year
    }

    data class DirectoryRecord(
            val length: Int,
            val extendedAttributeLength: Int,
            val extent: Int,
            val size: Int,
            val date: DateStruct,
            val flags: Int,
            val fileUnitSize: Int,
            val interleave: Int,
            val volumeSequenceNumber: Int,
            val name: String
    ) {
        val offset: Long = extent.toLong() * SECTOR_SIZE
        val isDirectory = (flags and 2) != 0

        companion object {
            operator fun invoke(_s: SyncStream): DirectoryRecord {
                val length = _s.readU8()
                val s = _s.readStream((length - 1).toLong())

                return DirectoryRecord(
                        length = length,
                        extendedAttributeLength = s.readU8(),
                        extent = s.readU32_le_be(),
                        size = s.readU32_le_be(),
                        date = DateStruct(s),
                        flags = s.readU8(),
                        fileUnitSize = s.readU8(),
                        interleave = s.readU8(),
                        volumeSequenceNumber = s.readU16_le_be(),
                        name = s.readTextWithLength()
                )
            }
        }
    }
}