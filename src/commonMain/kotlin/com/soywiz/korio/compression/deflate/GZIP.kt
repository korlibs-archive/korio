package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.util.BitReader
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.checksum.*

val GZIP = GZIPBase(true)
val GZIPNoCrc = GZIPBase(false)

open class GZIPBase(val checkCrc: Boolean) : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputStreamWithLength, o: AsyncOutputStream) {
		val s = BitReader(i)
		s.prepareBigChunk()
		if (s.su8() != 31 || s.su8() != 139) error("Not a GZIP file")
		val method = s.su8()
		if (method != 8) error("Just supported deflate in GZIP")
		val ftext = s.sreadBit()
		val fhcrc = s.sreadBit()
		val fextra = s.sreadBit()
		val fname = s.sreadBit()
		val fcomment = s.sreadBit()
		val reserved = s.readBits(3)
		val mtime = s.su32LE()
		val xfl = s.su8()
		val os = s.su8()
		val extra = if (fextra) s.abytes(s.su16LE()) else byteArrayOf()
		val name = if (fname) s.strz() else null
		val comment = if (fcomment) s.strz() else null
		val crc16 = if (fhcrc) s.su16LE() else 0
		var chash = CRC32.initialValue
		var csize = 0
		Deflate.uncompress(s, object : AsyncOutputStream by o {
			override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
				chash = CRC32.update(chash, buffer, offset, len)
				csize += len
				o.write(buffer, offset, len)
			}
		})
		s.prepareBigChunk()
		val crc32 = s.su32LE()
		val size = s.su32LE()
		if (checkCrc) {
			if (chash != crc32) invalidOp("CRC32 doesn't match ${chash.hex} != ${crc32.hex}")
			if (csize != size) invalidOp("Size doesn't match ${csize.hex} != ${size.hex}")
		}
	}

	override suspend fun compress(
		i: AsyncInputStreamWithLength,
		o: AsyncOutputStream,
		context: CompressionContext
	) {
		o.write8(31) // MAGIC[0]
		o.write8(139) // MAGIC[1]
		o.write8(8) // METHOD=8 (deflate)
		o.write8(0) // Presence bits
		o.write32LE(0) // Time
		o.write8(0) // xfl
		o.write8(0) // os

		var size = 0
		var crc32 = CRC32.initialValue
		Deflate.compress(object : AsyncInputStreamWithLength by i {
			override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
				val read = i.read(buffer, offset, len)
				if (read > 0) {
					crc32 = CRC32.update(crc32, buffer, offset, len)
					size += read
				}
				return read
			}
		}, o, context)
		o.write32LE(crc32)
		o.write32LE(size)
	}
}
