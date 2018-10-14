package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

val GZIP = GZIPBase(true)
val GZIPNoCrc = GZIPBase(false)

open class GZIPBase(val checkCrc: Boolean) : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
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
		val mtime = s.su32_le()
		val xfl = s.su8()
		val os = s.su8()
		val extra = if (fextra) s.abytes(s.su16_le()) else byteArrayOf()
		val name = if (fname) s.strz() else null
		val comment = if (fcomment) s.strz() else null
		val crc16 = if (fhcrc) s.su16_le() else 0
		var chash = CRC32.INITIAL
		var csize = 0
		Deflate.uncompress(s, object : AsyncOutputStream by o {
			override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
				chash = CRC32.update(chash, buffer, offset, len)
				csize += len
				o.write(buffer, offset, len)
			}
		})
		s.prepareBigChunk()
		val crc32 = s.su32_le()
		val size = s.su32_le()
		if (checkCrc) {
			if (chash != crc32) invalidOp("CRC32 doesn't match ${chash.hex} != ${crc32.hex}")
			if (csize != size) invalidOp("Size doesn't match ${csize.hex} != ${size.hex}")
		}
	}

	override suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext
	) {
		o.write8(31) // MAGIC[0]
		o.write8(139) // MAGIC[1]
		o.write8(8) // METHOD=8 (deflate)
		o.write8(0) // Presence bits
		o.write32_le(0) // Time
		o.write8(0) // xfl
		o.write8(0) // os

		var size = 0
		var crc32 = CRC32.INITIAL
		Deflate.compress(object : AsyncInputWithLengthStream by i {
			override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
				val read = i.read(buffer, offset, len)
				if (read > 0) {
					crc32 = CRC32.update(crc32, buffer, offset, len)
					size += read
				}
				return read
			}
		}, o, context)
		o.write32_le(crc32)
		o.write32_le(size)
	}
}
