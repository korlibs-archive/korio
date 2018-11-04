package com.soywiz.korio.file.std

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.coroutines.*
import kotlin.math.*

suspend fun ZipVfs(s: AsyncStream, zipFile: VfsFile? = null): VfsFile =
	ZipVfs(s, zipFile, caseSensitive = true)

suspend fun ZipVfs(s: AsyncStream, zipFile: VfsFile? = null, caseSensitive: Boolean = true): VfsFile {
	//val s = zipFile.open(VfsOpenMode.READ)
	var endBytes = EMPTY_BYTE_ARRAY

	val PK_END = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
	var pk_endIndex = -1

	for (chunkSize in listOf(0x16, 0x100, 0x1000, 0x10000)) {
		s.setPosition(max(0L, s.getLength() - chunkSize))
		endBytes = s.readBytesExact(max(chunkSize, s.getAvailable().toIntClamp()))
		pk_endIndex = endBytes.indexOf(PK_END)
		if (pk_endIndex >= 0) break
	}

	class ZipEntry2(
		val path: String,
		val compressionMethod: Int,
		val isDirectory: Boolean,
		val time: DosFileDateTime,
		val offset: Int,
		val inode: Long,
		val headerEntry: AsyncStream,
		val compressedSize: Long,
		val uncompressedSize: Long
	)

	fun ZipEntry2?.toStat(file: VfsFile): VfsStat {
		val vfs = file.vfs
		return if (this != null) {
			vfs.createExistsStat(
				file.path,
				isDirectory = isDirectory,
				size = uncompressedSize,
				inode = inode,
				createTime = this.time.utc
			)
		} else {
			vfs.createNonExistsStat(file.path)
		}
	}

	fun String.normalizeName() = if (caseSensitive) this.trim('/') else this.trim('/').toLowerCase()

	if (pk_endIndex < 0) throw IllegalArgumentException("Not a zip file")

	val data = endBytes.copyOfRange(pk_endIndex, endBytes.size).openSync()

	val files = LinkedHashMap<String, ZipEntry2>()
	val filesPerFolder = LinkedHashMap<String, MutableMap<String, ZipEntry2>>()

	@Suppress("UNUSED_VARIABLE")
	data.apply {
		//println(s)
		if (readS32_be() != 0x504B_0506) throw IllegalStateException("Not a zip file")
		val diskNumber = readU16_le()
		val startDiskNumber = readU16_le()
		val entriesOnDisk = readU16_le()
		val entriesInDirectory = readU16_le()
		val directorySize = readS32_le()
		val directoryOffset = readS32_le()
		val commentLength = readU16_le()

		//println("Zip: $entriesInDirectory")

		val ds = s.sliceWithSize(directoryOffset.toLong(), directorySize.toLong()).readAvailable().openSync()
		ds.apply {
			for (n in 0 until entriesInDirectory) {
				if (readS32_be() != 0x504B_0102) throw IllegalStateException("Not a zip file record")
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

				val baseFolder = normalizedName.substringBeforeLast('/', "")
				val baseName = normalizedName.substringAfterLast('/')

				val folder = filesPerFolder.getOrPut(baseFolder) { LinkedHashMap() }
				val entry = ZipEntry2(
					path = name,
					compressionMethod = compressionMethod,
					isDirectory = isDirectory,
					time = DosFileDateTime(fileTime, fileDate),
					inode = n.toLong(),
					offset = headerOffset,
					headerEntry = s.sliceStart(headerOffset.toUInt()),
					compressedSize = compressedSize.toUInt(),
					uncompressedSize = uncompressedSize.toUInt()
				)
				val components = listOf("") + PathInfo(normalizedName).getPathFullComponents()
				for (m in 1 until components.size) {
					val f = components[m - 1]
					val c = components[m]
					if (c !in files) {
						val folder2 = filesPerFolder.getOrPut(f) { LinkedHashMap() }
						val entry2 = ZipEntry2(
							path = c,
							compressionMethod = 0,
							isDirectory = true,
							time = DosFileDateTime(0, 0),
							inode = 0L,
							offset = 0,
							headerEntry = byteArrayOf().openAsync(),
							compressedSize = 0L,
							uncompressedSize = 0L
						)
						folder2[PathInfo(c).basename] = entry2
						files[c] = entry2
					}
				}
				//println(components)
				folder[baseName] = entry
				files[normalizedName] = entry
			}
		}
		files[""] = ZipEntry2(
			path = "",
			compressionMethod = 0,
			isDirectory = true,
			time = DosFileDateTime(0, 0),
			inode = 0L,
			offset = 0,
			headerEntry = byteArrayOf().openAsync(),
			compressedSize = 0L,
			uncompressedSize = 0L
		)
		Unit
	}

	class Impl : Vfs() {
		val vfs = this

		override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
			val entry = files[path.normalizeName()] ?: throw com.soywiz.korio.FileNotFoundException("Path: '$path'")
			if (entry.isDirectory) throw com.soywiz.korio.IOException("Can't open a zip directory for $mode")
			val base = entry.headerEntry.sliceStart()
			@Suppress("UNUSED_VARIABLE")
			return base.run {
				if (this.getAvailable() < 16) throw IllegalStateException("Chunk to small to be a ZIP chunk")
				if (readS32_be() != 0x504B_0304) throw IllegalStateException("Not a zip file")
				val version = readU16_le()
				val flags = readU16_le()
				val compressionType = readU16_le()
				val fileTime = readU16_le()
				val fileDate = readU16_le()
				val crc = readS32_le()
				val compressedSize = readS32_le()
				val uncompressedSize = readS32_le()
				val fileNameLength = readU16_le()
				val extraLength = readU16_le()
				val name = readString(fileNameLength)
				val extra = readBytesExact(extraLength)
				val compressedData = readSlice(entry.compressedSize)

				when (entry.compressionMethod) {
					0 -> compressedData
					else -> {
						val compressed = UncompressAsyncStream(
							when (entry.compressionMethod) {
								8 -> Deflate
								else -> TODO("Not implemented zip method ${entry.compressionMethod}")
							}, compressedData, uncompressedSize.toLong()
						).readAll()

						val computedCrc = CRC32.compute(compressed)
						if (computedCrc != crc) error("Uncompressed file crc doesn't match: expected=${crc.hex}, actual=${computedCrc.hex}")

						compressed.openAsync()
					}
				}
			}
		}

		override suspend fun stat(path: String): VfsStat {
			return files[path.normalizeName()].toStat(this@Impl[path])
		}

		override suspend fun list(path: String): SuspendingSequence<VfsFile> {
			return asyncGenerate(coroutineContext) {
				for ((_, entry) in filesPerFolder[path.normalizeName()] ?: LinkedHashMap()) {
					//yield(entry.toStat(this@Impl[entry.path]))
					yield(vfs[entry.path])
				}
			}
		}

		override fun toString(): String = "ZipVfs($zipFile)"
	}

	return Impl().root
}

private class DosFileDateTime(var dosTime: Int, var dosDate: Int) {
	val seconds: Int get() = 2 * dosTime.getBits(0, 5)
	val minutes: Int get() = dosTime.getBits(5, 6)
	val hours: Int get() = dosTime.getBits(11, 5)
	val day: Int get() = dosDate.getBits(0, 5)
	val month1: Int get() = dosDate.getBits(5, 4)
	val fullYear: Int get() = 1980 + dosDate.getBits(9, 7)

	init {
		//println("DosFileDateTime: $fullYear-$month1-$day $hours-$minutes-$seconds")
	}

	val utc: DateTime = DateTime.createAdjusted(fullYear, month1, day, hours, minutes, seconds)
}

suspend fun VfsFile.openAsZip() =
	ZipVfs(this.open(com.soywiz.korio.file.VfsOpenMode.READ), this)

suspend fun VfsFile.openAsZip(caseSensitive: Boolean) =
	ZipVfs(
		this.open(com.soywiz.korio.file.VfsOpenMode.READ),
		this,
		caseSensitive = caseSensitive
	)

suspend fun AsyncStream.openAsZip() = ZipVfs(this)
suspend fun AsyncStream.openAsZip(caseSensitive: Boolean) =
	ZipVfs(this, caseSensitive = caseSensitive)

suspend fun VfsFile.createZipFromTree(): ByteArray {
	val buf = ByteArrayBuffer()
	val mem = MemorySyncStream(buf)
	this.createZipFromTreeTo(mem.toAsync())
	return buf.toByteArray()
}

suspend fun VfsFile.createZipFromTreeTo(s: AsyncStream) {
	val entries = arrayListOf<ZipEntry>()
	addZipFileEntryTree(s, this, entries)
	val directoryStart = s.position

	for (entry in entries) {
		addDirEntry(s, entry)
	}
	val directoryEnd = s.position
	val comment = byteArrayOf()

	s.writeString("PK\u0005\u0006")
	s.write16_le(0)
	s.write16_le(0)
	s.write16_le(entries.size)
	s.write16_le(entries.size)
	s.write32_le((directoryEnd - directoryStart).toInt())
	s.write32_le(directoryStart.toInt())
	s.write16_le(comment.size)
	s.writeBytes(comment)
}

private suspend fun addZipFileEntry(s: AsyncStream, entry: VfsFile): ZipEntry {
	val size = entry.size().toInt()
	val versionMadeBy = 0x314
	val extractVersion = 10
	val flags = 2048
	//val compressionMethod = 8 // Deflate
	val compressionMethod = 0 // Store
	val date = 0
	val time = 0
	val crc32 = entry.hashSync(AsyncHash.CRC32).readS32_le(0)

	val name = entry.fullname.trim('/')
	val nameBytes = name.toByteArray(UTF8)
	val extraBytes = byteArrayOf()
	val compressedSize = size
	val uncompressedSize = size

	val headerOffset = s.position
	s.writeString("PK\u0003\u0004")
	s.write16_le(extractVersion)
	s.write16_le(flags)
	s.write16_le(compressionMethod)
	s.write16_le(date)
	s.write16_le(time)
	s.write32_le(crc32)
	s.write32_le(compressedSize)
	s.write32_le(uncompressedSize)
	s.write16_le(nameBytes.size)
	s.write16_le(extraBytes.size)
	s.writeBytes(nameBytes)
	s.writeBytes(extraBytes)
	s.writeFile(entry)

	return ZipEntry(
		versionMadeBy = versionMadeBy,
		extractVersion = extractVersion,
		headerOffset = headerOffset,
		compressionMethod = compressionMethod,
		flags = flags,
		date = date,
		time = time,
		crc32 = crc32,
		compressedSize = compressedSize,
		uncompressedSize = uncompressedSize,
		nameBytes = nameBytes,
		extraBytes = extraBytes,
		commentBytes = byteArrayOf(),
		diskNumberStart = 0,
		internalAttributes = 0,
		externalAttributes = 0
	)
}

private suspend fun addZipFileEntryTree(s: AsyncStream, entry: VfsFile, entries: MutableList<ZipEntry>) {
	if (entry.isDirectory()) {
		for (it in entry.list()) addZipFileEntryTree(s, it, entries)
	} else {
		entries += addZipFileEntry(s, entry)
	}
}

private class ZipEntry(
	val versionMadeBy: Int,
	val extractVersion: Int,
	val headerOffset: Long,
	val compressionMethod: Int,
	val flags: Int,
	val date: Int,
	val time: Int,
	val crc32: Int,
	val compressedSize: Int,
	val uncompressedSize: Int,
	val nameBytes: ByteArray,
	val extraBytes: ByteArray,
	val diskNumberStart: Int,
	val internalAttributes: Int,
	val externalAttributes: Int,
	val commentBytes: ByteArray
)

private suspend fun addDirEntry(s: AsyncStream, e: ZipEntry) {
	s.writeString("PK\u0001\u0002")
	s.write16_le(e.versionMadeBy)
	s.write16_le(e.extractVersion)
	s.write16_le(e.flags)
	s.write16_le(e.compressionMethod)
	s.write16_le(e.date)
	s.write16_le(e.time)
	s.write32_le(e.crc32)
	s.write32_le(e.compressedSize)
	s.write32_le(e.uncompressedSize)
	s.write16_le(e.nameBytes.size)
	s.write16_le(e.extraBytes.size)
	s.write16_le(e.commentBytes.size)
	s.write16_le(e.diskNumberStart)
	s.write16_le(e.internalAttributes)
	s.write32_le(e.externalAttributes)
	s.write32_le(e.headerOffset.toInt())
	s.writeBytes(e.nameBytes)
	s.writeBytes(e.extraBytes)
	s.writeBytes(e.commentBytes)
}
