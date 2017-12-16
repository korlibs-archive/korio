package com.soywiz.korio.vfs

import com.soywiz.klock.Klock
import com.soywiz.kmem.arraycopy
import com.soywiz.korio.IOException
import com.soywiz.korio.async.SuspendingSequence
import com.soywiz.korio.async.toAsync
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.fromHexString
import com.soywiz.korio.util.hex
import kotlin.math.max
import kotlin.math.min

interface SimpleStorage {
	suspend fun get(key: String): String?
	suspend fun set(key: String, value: String)
	suspend fun remove(key: String)
}

private class StorageFiles(val storage: SimpleStorage) {
	companion object {
		val CHUNK_SIZE = 16 * 1024 // 16K
	}

	fun getStatsKey(fileName: String) = "korio_stats_v1_$fileName"
	fun getChunkKey(fileName: String, chunk: Int) = "korio_chunk${chunk}_v1_$fileName"

	data class EntryInfo(
		val isFile: Boolean,
		val size: Long = 0L,
		val children: List<String> = listOf(),
		val createdTime: Long = 0L,
		val modifiedTime: Long = 0L
	) {
		val isDirectory get() = !isFile
	}

	suspend fun setEntryInfo(fileName: String, info: EntryInfo) {
		setEntryInfo(fileName, info.isFile, info.size, info.children, info.createdTime, info.modifiedTime)
	}

	suspend fun setEntryInfo(fileName: String, isFile: Boolean, size: Long, children: List<String>, createdTime: Long = 0L, modifiedTime: Long = 0L) {
		val oldEntry = getEntryInfo(fileName)

		if (oldEntry != null) {
			// @TODO: Prune old chunks/children!
		}

		storage.set(getStatsKey(fileName), Json.stringify(hashMapOf(
			EntryInfo::isFile.name to isFile,
			EntryInfo::size.name to size.toDouble(),
			EntryInfo::children.name to children,
			EntryInfo::createdTime.name to createdTime.toDouble(),
			EntryInfo::modifiedTime.name to modifiedTime.toDouble()
		)))
	}

	suspend fun hasEntryInfo(fileName: String): Boolean = getEntryInfo(fileName) != null

	suspend fun getEntryInfo(fileName: String): EntryInfo? {
		val info = storage.get(getStatsKey(fileName)) ?: return null
		val di = Json.parse(info) as Map<String, Any>
		return EntryInfo(
			di[EntryInfo::isFile.name]!! as Boolean,
			(di[EntryInfo::size.name]!! as Number).toLong(),
			(di[EntryInfo::children.name] as Iterable<String>).toList(),
			(di[EntryInfo::createdTime.name] as Number).toLong(),
			(di[EntryInfo::modifiedTime.name] as Number).toLong()
		)
	}

	suspend fun removeEntryInfo(fileName: String): Boolean {
		val entry = getEntryInfo(fileName)
		if (entry != null) {
			for (child in entry.children) removeEntryInfo(child)
			return true
		} else {
			return false
		}
	}

	suspend fun setFileChunk(fileName: String, chunk: Int, data: ByteArray) = run {
		storage.set(getChunkKey(fileName, chunk), data.hex)
	}

	suspend fun getFileChunk(fileName: String, chunk: Int): ByteArray? = storage.get(getChunkKey(fileName, chunk))?.fromHexString()

	suspend fun writeData(fileName: String, position: Long, buffer: ByteArray, offset: Int, len: Int) {
		var pending = len
		var apos = position
		var aoffset = offset
		while (pending > 0) {
			val chunk = (apos / CHUNK_SIZE).toInt()
			val inChunk = (apos % CHUNK_SIZE).toInt()
			val c = getFileChunk(fileName, chunk) ?: byteArrayOf()
			val available = CHUNK_SIZE - inChunk
			val written = min(available, pending)
			if (written <= 0) invalidOp("Unexpected written")
			val cc = c.copyOf(inChunk + written)
			arraycopy(buffer, aoffset, cc, inChunk, written)
			setFileChunk(fileName, chunk, cc)
			pending -= written
			apos += written
			aoffset += written
		}
	}

	suspend fun readData(fileName: String, position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val info = getEntryInfo(fileName) ?: return -1
		if (position >= info.size) return 0
		val chunk = (position / CHUNK_SIZE).toInt()
		val inChunk = (position % CHUNK_SIZE).toInt()
		val c = getFileChunk(fileName, chunk) ?: return 0
		val available = c.size - inChunk
		val read = min(available, len)
		arraycopy(c, inChunk, buffer, offset, read)
		return read
	}
}

class MapLikeStorageVfs(val storage: SimpleStorage) : Vfs() {
	private val files = StorageFiles(storage)
	private var initialized = false

	suspend private fun initOnce() {
		if (!initialized) {
			initialized = true
			// Create root
			if (!files.hasEntryInfo("/")) {
				files.setEntryInfo("/", StorageFiles.EntryInfo(isFile = false, size = 0L))
			}
		}
	}

	fun String.normalizePath() = "/" + this.trim('/').replace('\\', '/')

	suspend fun remove(path: String, directory: Boolean): Boolean {
		initOnce()
		val npath = path.normalizePath()
		val entry = files.getEntryInfo(npath) ?: return false
		if (entry.isDirectory == directory) {
			if (directory && entry.children.isNotEmpty()) throw IOException("Directory '$npath' is not empty")
			return files.removeEntryInfo(npath)
		} else {
			return false
		}
	}

	suspend override fun rmdir(path: String): Boolean = remove(path, directory = true)
	suspend override fun delete(path: String): Boolean = remove(path, directory = false)

	suspend override fun stat(path: String): VfsStat {
		initOnce()
		val npath = path.normalizePath()
		val entry = files.getEntryInfo(npath) ?: return createNonExistsStat(path)
		return createExistsStat(path, entry.isDirectory, entry.size, createTime = entry.createdTime, modifiedTime = entry.modifiedTime)
	}

	suspend private fun ensureParentDirectory(nparent: String, npath: String): StorageFiles.EntryInfo {
		if (!files.hasEntryInfo(nparent)) throw IOException("Parent directory '$nparent' for file '$npath' doesn't exists")
		val parent = files.getEntryInfo(nparent)!!
		if (parent.isFile) throw IOException("'$nparent' is a file")
		return parent
	}

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		initOnce()
		val npath = path.normalizePath()
		val nparent = PathInfo(npath).folder.normalizePath()
		if (!files.hasEntryInfo(nparent)) mkdir(nparent, attributes) // Create Parents
		val parent = ensureParentDirectory(nparent, npath)
		val now = Klock.currentTimeMillis()
		if (files.hasEntryInfo(npath)) return false
		files.setEntryInfo(nparent, parent.copy(children = parent.children + npath))
		files.setEntryInfo(npath, isFile = false, size = 0L, children = listOf(), createdTime = now, modifiedTime = now)
		return true
	}

	suspend override fun list(path: String): SuspendingSequence<VfsFile> {
		initOnce()
		val npath = path.normalizePath()
		val entry = files.getEntryInfo(npath) ?: throw IOException("Can't find '$path'")
		return entry.children.map { VfsFile(this, it) }.toAsync()
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		initOnce()
		val npath = path.normalizePath()
		val nparent = PathInfo(npath).folder.normalizePath()
		val parent = ensureParentDirectory(nparent, npath)

		if (!files.hasEntryInfo(npath)) {
			if (!mode.createIfNotExists) throw IOException("File '$npath' doesn't exists")
			files.setEntryInfo(nparent, parent.copy(children = parent.children + npath))
		}

		val now = Klock.currentTimeMillis()
		var info = files.getEntryInfo(npath) ?: StorageFiles.EntryInfo(isFile = true, size = 0L, children = listOf(), createdTime = now, modifiedTime = now)
		if (info.isDirectory) throw IOException("Can't open a directory")

		return object : AsyncStreamBase() {
			suspend private fun updateInfo(newInfo: StorageFiles.EntryInfo) {
				if (info != newInfo) {
					info = newInfo
					files.setEntryInfo(npath, info)
				}
			}

			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				return files.readData(npath, position, buffer, offset, len)
			}

			suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				files.writeData(npath, position, buffer, offset, len)
				updateInfo(info.copy(size = max(info.size, position + len)))
			}

			suspend override fun setLength(value: Long) {
				updateInfo(info.copy(size = value))
			}

			suspend override fun getLength(): Long {
				return info.size
			}

			suspend override fun close() {
			}
		}.toAsyncStream()
	}

	suspend override fun touch(path: String, time: Long, atime: Long) {
		initOnce()
		val npath = path.normalizePath()
		if (files.hasEntryInfo(npath)) {
			files.setEntryInfo(npath, files.getEntryInfo(npath)!!.copy(modifiedTime = time))
		}
	}
}