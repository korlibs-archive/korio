package com.soywiz.korio.compression.deflate

import com.soywiz.kmem.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

object ZLib : CompressionMethod {
	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		val s = BitReader(i)
		//println("Zlib.uncompress.available[0]:" + s.available())
		s.prepareBigChunk()
		val cmf = s.su8()
		val flg = s.su8()

		if ((cmf * 256 + flg) % 31 != 0) error("bad zlib header")

		val compressionMethod = cmf.extract(0, 4)
		if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
		val windowBits = (cmf.extract(4, 4) + 8)
		val fcheck = flg.extract(0, 5)
		val hasDict = flg.extract(5)
		val flevel = flg.extract(6, 2)

		var dictid = 0
		if (hasDict) {
			dictid = s.su32LE()
			TODO("Unsupported custom dictionaries (Provided DICTID=$dictid)")
		}
		//println("ZLib.uncompress[2]")

		//s.alignbyte()
		var chash = Adler32.INITIAL
		Deflate(windowBits).uncompress(s, object : AsyncOutputStream {
			override suspend fun close() {
				o.close()
			}

			override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
				o.write(buffer, offset, len)
				chash = Adler32.update(chash, buffer, offset, len)
				//println("UNCOMPRESS:'" + buffer.sliceArray(offset until (offset + len)).toString(UTF8) + "':${chash.hex32}")
			}
		})
		//println("ZLib.uncompress[3]")

		s.prepareBigChunk()
		val adler32 = s.su32BE()
		//println("Zlib.uncompress.available[1]:" + s.available())
		if (chash != adler32) invalidOp("Adler32 doesn't match ${chash.hex} != ${adler32.hex}")
		//println("ZLib.uncompress[4]")
	}

	override suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext
	) {
		//val level = context.level
		val level = 0
		val slidingBits = 15

		val cmf = 0x8 or ((slidingBits - 8) shl 4) // METHOD=8, BITS=7+8
		val flg = 0x00 or (level shl 6) // FCHECK=0, HASDICT=0, LEVEL = context.level

		// @TODO: This is a brute-force. Compute with an expression instead
		var fcheck = 0
		for (n in 0 until 32) {
			if ((cmf * 256 + (flg or n)) % 31 == 0) {
				fcheck = n
				break
			}
		}

		o.write8(cmf)
		o.write8(flg or fcheck)

		var chash = Adler32.INITIAL
		Deflate(slidingBits).compress(object : AsyncInputWithLengthStream by i {
			override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
				val read = i.read(buffer, offset, len)
				if (read > 0) {
					chash = Adler32.update(chash, buffer, offset, read)
					//println("COMPRESS:'" + buffer.sliceArray(offset until (offset + len)).toString(UTF8) + "':${chash.hex32}")
				}
				return read
			}
		}, o)
		o.write32BE(chash)
	}
}
