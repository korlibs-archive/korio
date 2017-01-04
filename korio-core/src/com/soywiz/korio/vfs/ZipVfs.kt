package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.AsyncCloseable
import com.soywiz.korio.util.getBits
import com.soywiz.korio.util.indexOf
import com.soywiz.korio.util.toUInt
import java.io.FileNotFoundException
import java.util.*
import java.util.zip.Inflater

suspend fun ZipVfs(s: AsyncStream, zipFile: VfsFile? = null) = asyncFun {
	//val s = zipFile.open(VfsOpenMode.READ)
	var endBytes = ByteArray(0)

	val PK_END = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
	var pk_endIndex = -1

	for (chunkSize in listOf(0x16, 0x100, 0x1000, 0x10000)) {
		s.setPosition(s.getLength() - chunkSize)
		endBytes = s.readBytesExact(chunkSize)
		pk_endIndex = endBytes.indexOf(PK_END)
		if (pk_endIndex >= 0) break
	}

	if (pk_endIndex < 0) throw IllegalArgumentException("Not a zip file")

	val data = Arrays.copyOfRange(endBytes, pk_endIndex, endBytes.size).openSync()

	fun String.normalizeName() = this.trim('/')

	class ZipEntry(
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

	fun ZipEntry?.toStat(file: VfsFile): VfsStat {
		val vfs = file.vfs
		return if (this != null) {
			vfs.createExistsStat(file.path, isDirectory = isDirectory, size = uncompressedSize, inode = inode, createTime = this.time.utcTimestamp)
		} else {
			vfs.createNonExistsStat(file.path)
		}
	}

	val files = hashMapOf<String, ZipEntry>()
	val filesPerFolder = hashMapOf<String, HashMap<String, ZipEntry>>()

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

				val folder = filesPerFolder.getOrPut(baseFolder) { hashMapOf() }
				val entry = ZipEntry(
					path = name,
					compressionMethod = compressionMethod,
					isDirectory = isDirectory,
					time = DosFileDateTime(fileTime, fileDate),
					inode = n.toLong(),
					offset = headerOffset,
					headerEntry = s.sliceWithStart(headerOffset.toUInt()),
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
			val base = entry.headerEntry.slice()
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
					8 -> InflateAsyncStream(compressedData, Inflater(true), uncompressedSize.toLong()).toAsyncStream()
					else -> TODO("Not implemented zip method ${entry.compressionMethod}")
				}
			}
		}

		suspend override fun stat(path: String): VfsStat {
			return files[path.normalizeName()].toStat(this@Impl[path])
		}

		suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
			for ((name, entry) in filesPerFolder[path.normalizeName()] ?: hashMapOf()) {
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
	val utcTimestamp: Long by lazy { Date.UTC(year - 1900, month - 1, day, hours, minutes, seconds) }
	val javaDate: Date by lazy { Date(utcTimestamp) }
}

suspend fun VfsFile.openAsZip() = asyncFun { ZipVfs(this.open(VfsOpenMode.READ), this) }
suspend fun AsyncStream.openAsZip() = asyncFun { ZipVfs(this) }

class InflateAsyncStream(val base: AsyncStream, val inflater: Inflater, val uncompressedSize: Long? = null) : AsyncInputStream, AsyncLengthStream, AsyncCloseable {
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
		if (inflater.needsInput()) {
			inflater.setInput(base.readBytes(1024))
		}
		executeInWorker {
			inflater.inflate(buffer, offset, len)
		}
	}

	suspend override fun setLength(value: Long) = throw UnsupportedOperationException()
	suspend override fun getLength(): Long = uncompressedSize ?: throw UnsupportedOperationException()

	suspend override fun close() = asyncFun {
		inflater.end()
	}
}