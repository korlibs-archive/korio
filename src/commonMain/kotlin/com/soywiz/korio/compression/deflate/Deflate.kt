package com.soywiz.korio.compression.deflate

import com.soywiz.kmem.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.stream.*
import kotlin.math.*

open class Deflate(val windowBits: Int) : CompressionMethod {
	override suspend fun compress(
		i: AsyncInputWithLengthStream,
		o: AsyncOutputStream,
		context: CompressionContext
	) {
		while (i.hasAvailable()) {
			val available = i.getAvailable()
			val chunkSize = min(available, 0xFFFFL).toInt()
			o.write8(if (chunkSize >= available) 1 else 0)
			o.write16_le(chunkSize)
			o.write16_le(chunkSize.inv())
			//for (n in 0 until chunkSize) o.write8(i.readU8())
			o.writeBytes(i.readBytesExact(chunkSize))
		}
	}

	override suspend fun uncompress(i: AsyncInputWithLengthStream, o: AsyncOutputStream) {
		uncompress(BitReader(i), o)
		//println("uncompress[6]")
	}

	suspend fun uncompress(reader: BitReader, out: AsyncOutputStream) {
		val tempResult = HuffmanTree.Result(0, 0, 0)
		val ring = SlidingWindow(windowBits)
		val sout = SlidingWindowWithOutput(ring, out)
		var lastBlock = false
		//println("uncompress[0]")
		while (!lastBlock) {
			if (reader.requirePrepare) reader.prepareBigChunk()
			//println("uncompress[1]")

			lastBlock = reader.sreadBit()
			val btype = reader.readBits(2)
			if (btype !in 0..2) error("invalid bit")
			if (btype == 0) {
				//println("uncompress[2]")
				reader.discardBits()
				if (reader.requirePrepare) reader.prepareBigChunk()
				val len = reader.su16_le()
				val nlen = reader.su16_le()
				val nnlen = nlen.inv() and 0xFFFF
				if (len != nnlen) error("Invalid deflate stream: len($len) != ~nlen($nnlen) :: nlen=$nlen")
				val bytes = reader.abytes(len)
				sout.putOut(bytes, 0, len)
			} else {
				//println("uncompress[3]")
				if (reader.requirePrepare) reader.prepareBigChunk()
				val (tree, dist) = if (btype == 1) FIXED_TREE_DIST else readDynamicTree(reader)
				while (true) {
					if (reader.requirePrepare) reader.prepareBigChunk()
					val value = tree.sreadOneValue(reader, tempResult)
					if (value == 256) break
					if (value < 256) {
						sout.putOut(value.toByte())
					} else {
						if (reader.requirePrepare) reader.prepareBigChunk()
						val lengthInfo = INFOS_LZ[value - 257]
						val lengthExtra = reader.readBits(lengthInfo.extra)
						val distanceData = dist.sreadOneValue(reader, tempResult)
						val distanceInfo = INFOS_LZ2[distanceData]
						val distanceExtra = reader.readBits(distanceInfo.extra)
						val distance = distanceInfo.offset + distanceExtra
						val length = lengthInfo.offset + lengthExtra
						sout.getPutCopyOut(distance, length)
					}
					if (sout.mustFlush) sout.flush()
				}
			}
		}
		//println("uncompress[4]")
		sout.flush(finish = true)
		//println("uncompress[5]")
	}

	private fun readDynamicTree(reader: BitReader): Pair<HuffmanTree, HuffmanTree> {
		val tempResult = HuffmanTree.Result(0, 0, 0)
		val hlit = reader.readBits(5) + 257
		val hdist = reader.readBits(5) + 1
		val hclen = reader.readBits(4) + 4
		val codeLenCodeLen = IntArray(19)
		for (i in 0 until hclen) codeLenCodeLen[HCLENPOS[i]] = reader.readBits(3)
		//console.info(codeLenCodeLen);
		val codeLen = HuffmanTree.fromLengths(codeLenCodeLen)
		val lengths = IntArray(hlit + hdist)
		var n = 0
		val hlithdist = hlit + hdist
		while (n < hlithdist) {
			val value = codeLen.sreadOneValue(reader, tempResult)
			if (value !in 0..18) error("Invalid")

			val len = when (value) {
				16 -> reader.readBits(2) + 3
				17 -> reader.readBits(3) + 3
				18 -> reader.readBits(7) + 11
				else -> 1
			}
			val vv = when (value) {
				16 -> lengths[n - 1]
				17 -> 0
				18 -> 0
				else -> value
			}

			lengths.fill(vv, n, n + len)
			n += len
		}
		return Pair(
			HuffmanTree.fromLengths(lengths.sliceArray(0 until hlit)),
			HuffmanTree.fromLengths(lengths.sliceArray(hlit until lengths.size))
		)
	}


	companion object : Deflate(15) {
		private data class Info(val extra: Int, val offset: Int)

		// @TODO: kotlin-native: by lazy not working with global state?

		//private val LENGTH0: IntArray by atomicLazy {
		//	IntArray(288).apply {
		//		for (n in 0..143) this[n] = 8
		//		for (n in 144..255) this[n] = 9
		//		for (n in 256..279) this[n] = 7
		//		for (n in 280..287) this[n] = 8
		//	}
		//}
		//
		//// https://www.ietf.org/rfc/rfc1951.txt
		//private val FIXED_TREE: HuffmanTree by atomicLazy { HuffmanTree.fromLengths(LENGTH0) }
		//private val FIXED_DIST: HuffmanTree by atomicLazy { HuffmanTree.fromLengths(IntArray(32) { 5 }) }

		private val LENGTH0: IntArray = IntArray(288).apply {
			for (n in 0..143) this[n] = 8
			for (n in 144..255) this[n] = 9
			for (n in 256..279) this[n] = 7
			for (n in 280..287) this[n] = 8
		}

		// https://www.ietf.org/rfc/rfc1951.txt
		private val FIXED_TREE: HuffmanTree = HuffmanTree.fromLengths(LENGTH0)
		private val FIXED_DIST: HuffmanTree = HuffmanTree.fromLengths(IntArray(32) { 5 })

		private val FIXED_TREE_DIST = FIXED_TREE to FIXED_DIST

		private val INFOS_LZ = arrayOf(
			Info(0, 3), Info(0, 4), Info(0, 5), Info(0, 6),
			Info(0, 7), Info(0, 8), Info(0, 9), Info(0, 10),
			Info(1, 11), Info(1, 13), Info(1, 15), Info(1, 17),
			Info(2, 19), Info(2, 23), Info(2, 27), Info(2, 31),
			Info(3, 35), Info(3, 43), Info(3, 51), Info(3, 59),
			Info(4, 67), Info(4, 83), Info(4, 99), Info(4, 115),
			Info(5, 131), Info(5, 163), Info(5, 195), Info(5, 227), Info(0, 258)
		)
		private val INFOS_LZ2 = arrayOf(
			Info(0, 1), Info(0, 2), Info(0, 3), Info(0, 4),
			Info(1, 5), Info(1, 7), Info(2, 9), Info(2, 13),
			Info(3, 17), Info(3, 25), Info(4, 33), Info(4, 49),
			Info(5, 65), Info(5, 97), Info(6, 129), Info(6, 193),
			Info(7, 257), Info(7, 385), Info(8, 513), Info(8, 769),
			Info(9, 1025), Info(9, 1537), Info(10, 2049), Info(10, 3073),
			Info(11, 4097), Info(11, 6145), Info(12, 8193), Info(12, 12289),
			Info(13, 16385), Info(13, 24577)
		)
		private val HCLENPOS = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)
	}
}

class SlidingWindowWithOutput(val sliding: SlidingWindow, val out: AsyncOutputStream) {
	// @TODO: Optimize with buffering and copying
	val bab = ByteArrayBuffer(8 * 1024)

	val output get() = bab.size
	val mustFlush get() = bab.size >= 4 * 1024

	fun getPutCopyOut(distance: Int, length: Int) {
		//print("LZ: distance=$distance, length=$length   :: ")
		for (n in 0 until length) {
			val v = sliding.getPut(distance)
			bab.append(v.toByte())
			//print("$v,")
		}
		//println()
	}

	fun putOut(bytes: ByteArray, offset: Int, len: Int) {
		//print("BYTES: $len ::")
		bab.append(bytes, offset, len)
		sliding.putBytes(bytes, offset, len)
		//for (n in 0 until len) print("${bytes[offset + n].toUnsigned()},")
		//println()
	}

	fun putOut(byte: Byte) {
		//println("BYTE: $byte")
		bab.append(byte)
		sliding.put(byte.toUnsigned())
	}

	suspend fun flush(finish: Boolean = false) {
		if (finish || mustFlush) {
			//print("FLUSH[$finish][${bab.size}]")
			//for (n in 0 until bab.size) print("${bab.data[n]},")
			//println()
			out.write(bab.data, 0, bab.size)
			bab.clear()
		}
	}
}
