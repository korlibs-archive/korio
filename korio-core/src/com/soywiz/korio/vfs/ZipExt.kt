package com.soywiz.korio.vfs

import com.soywiz.korio.crypto.AsyncHash
import com.soywiz.korio.crypto.hash
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.ByteArrayBuffer
import com.soywiz.korio.util.readS32_le

suspend fun VfsFile.treeCreateZip(): ByteArray {
	val buf = ByteArrayBuffer()
	val mem = MemorySyncStream(buf)
	this.treeCreateZipTo(mem.toAsync())
	return buf.toByteArray()
}

suspend private fun addZipFileEntry(s: AsyncStream, entry: VfsFile): ZipEntry {
	val size = entry.size().toInt()
	val versionMadeBy = 0x314
	val extractVersion = 10
	val flags = 2048
	//val compressionMethod = 8 // Deflate
	val compressionMethod = 0 // Store
	val date = 0
	val time = 0
	val crc32 = entry.hash(AsyncHash.CRC32).readS32_le(0)

	val name = entry.fullname.trim('/')
	val nameBytes = name.toByteArray(Charsets.UTF_8)
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

suspend private fun addZipFileEntryTree(s: AsyncStream, entry: VfsFile, entries: MutableList<ZipEntry>) {
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

suspend private fun addDirEntry(s: AsyncStream, e: ZipEntry) {
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

suspend fun VfsFile.treeCreateZipTo(s: AsyncStream) {
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

