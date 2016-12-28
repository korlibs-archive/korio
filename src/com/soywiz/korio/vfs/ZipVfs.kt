package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.getBits
import com.soywiz.korio.util.toUInt
import java.io.FileNotFoundException
import java.util.*
import java.util.zip.Inflater

suspend fun ZipVfs(s: AsyncStream, zipFile: VfsFile? = null) = asyncFun {
	//val s = zipFile.open(VfsOpenMode.READ)
	s.setPosition(s.getLength() - 0x16)
	val data = s.readBytesExact(0x16).openSync()

	fun String.normalizeName() = this.trim('/')

	class ZipEntry(
		val path: String,
		val compressionMethod: Int,
		val isDirectory: Boolean,
		val time: DosFileDateTime,
		val offset: Int,
		val inode: Long,
		val compressedData: AsyncStream,
		val compressedSize: Long,
		val uncompressedSize: Long
	)

	fun ZipEntry?.toStat(file: VfsFile): VfsStat {
		val vfs = file.vfs
		return if (this != null) {
			vfs.createExistsStat(file.path, isDirectory = isDirectory, size = uncompressedSize, inode = inode, createTime = this.time.timestamp)
		} else {
			vfs.createNonExistsStat(file.path)
		}
	}

	val files = hashMapOf<String, ZipEntry>()
	val filesPerFolder = hashMapOf<String, HashMap<String, ZipEntry>>()

	data.apply {
		if (readS32_be() != 0x504B_0506) throw IllegalStateException("Not a zip file")
		val diskNumber = readU16_le()
		val startDiskNumber = readU16_le()
		val entriesOnDisk = readU16_le()
		val entriesInDirectory = readU16_le()
		val directorySize = readS32_le()
		val directoryOffset = readS32_le()
		val commentLength = readU16_le()

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

				val folder = filesPerFolder.getOrPut(baseFolder) { hashMapOf() }
				val entry = ZipEntry(
					path = name,
					compressionMethod = compressionMethod,
					isDirectory = isDirectory,
					time = DosFileDateTime(fileTime, fileDate),
					inode = n.toLong(),
					offset = headerOffset,
					compressedData = s.sliceWithSize(headerOffset.toUInt(), compressedSize.toUInt()),
					compressedSize = compressedSize.toUInt(),
					uncompressedSize = uncompressedSize.toUInt()
				)
				folder[baseName] = entry
				files[normalizedName] = entry
			}
		}
	}

	class Impl : Vfs() {
		val vfs = this

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = asyncFun {
			val entry = files[path.normalizeName()] ?: throw FileNotFoundException(path)
			val base = entry.compressedData.slice()
			base.run {
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
				val extra = readBytes(extraLength)
				val compressedData = readSlice(entry.compressedSize)

				when (entry.compressionMethod) {
				// Uncompressed
					0 -> compressedData
				// Deflate
					8 -> InflateAsyncStream(compressedData, Inflater(true))
					else -> TODO("Not implemented zip method ${entry.compressionMethod}")
				}
			}
		}

		suspend override fun stat(path: String): VfsStat {
			return files[path.normalizeName()].toStat(this@Impl[path])
		}

		suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
			for ((name, entry) in filesPerFolder[path.normalizeName()]!!) {
				//yield(entry.toStat(this@Impl[entry.path]))
				yield(vfs[entry.path])
			}
		}

		override fun toString(): String = "ZipVfs($zipFile)"
	}

	Impl().root
}

private class DosFileDateTime(var time: Int, var date: Int) {
	val seconds: Int get() = 2 * date.getBits(0, 5)
	val minutes: Int get() = 2 * date.getBits(5, 6)
	val hours: Int get() = 2 * date.getBits(11, 5)
	val day: Int get() = date.getBits(0, 5)
	val month: Int get() = date.getBits(5, 4)
	val year: Int get() = 1980 + date.getBits(9, 7)
	val javaDate: Date by lazy { Date(year - 1900, month - 1, day, hours, minutes, seconds) }
	val timestamp: Long get() = javaDate.time
}

suspend fun VfsFile.openAsZip() = asyncFun { ZipVfs(this.open(VfsOpenMode.READ), this) }
suspend fun AsyncStream.openAsZip() = asyncFun { ZipVfs(this) }

class InflateAsyncStream(val base: AsyncStream, val inflater: Inflater) : AsyncStream() {
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
		if (inflater.needsInput()) {
			inflater.setInput(base.readBytes(1024))
		}
		executeInWorker {
			inflater.inflate(buffer, offset, len)
		}
	}
}