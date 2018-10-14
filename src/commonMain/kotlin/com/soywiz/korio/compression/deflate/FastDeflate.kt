package com.soywiz.korio.compression.deflate

import com.soywiz.kmem.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.stream.*

object FastDeflate {
	fun uncompress(input: ByteArray, outputHint: Int, method: String): ByteArray {
		return when (method) {
			"zlib" -> zlibUncompress(input, 0, expectedOutSize = outputHint)
			"deflate" -> uncompress(15, BitReader(input), expectedOutSize = outputHint).toByteArray()
			else -> error("Unsupported compression method $method")
		}
	}

	// https://www.ietf.org/rfc/rfc1951.txt
	private val FIXED_TREE: HuffmanTree = HuffmanTree().fromLengths(IntArray(288).apply {
		for (n in 0..143) this[n] = 8
		for (n in 144..255) this[n] = 9
		for (n in 256..279) this[n] = 7
		for (n in 280..287) this[n] = 8
	})
	private val FIXED_DIST: HuffmanTree = HuffmanTree().fromLengths(IntArray(32) { 5 })

	private val LEN_EXTRA = intArrayOf(
		0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 0, 0
	)

	private val LEN_BASE = intArrayOf(
		3, 4, 5, 6, 7, 8, 9, 10, 11, 13,
		15, 17, 19, 23, 27, 31, 35, 43, 51, 59,
		67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
	)

	private val DIST_EXTRA = intArrayOf(
		0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
	)

	private val DIST_BASE = intArrayOf(
		1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
		257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577, 0, 0
	)

	private val HCLENPOS = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

	fun zlibUncompress(i: ByteArray, offset: Int = 0, expectedOutSize: Int = 64): ByteArray {
		val s = BitReader(i, offset)
		val cmf = s.bits(8)
		val flg = s.bits(8)

		if ((cmf * 256 + flg) % 31 != 0) error("bad zlib header")

		val compressionMethod = cmf.extract(0, 4)
		if (compressionMethod != 8) error("Invalid zlib stream compressionMethod=$compressionMethod")
		val windowBits = (cmf.extract(4, 4) + 8)
		val fcheck = flg.extract(0, 5)
		val hasDict = flg.extract(5)
		val flevel = flg.extract(6, 2)
		var dictid = 0

		if (hasDict) {
			s.discardBits()
			dictid = s.u32be()
			TODO("Unsupported custom dictionaries (Provided DICTID=$dictid)")
		}
		val out = uncompress(windowBits, s, expectedOutSize = expectedOutSize).toByteArray()
		val chash = Adler32.update(Adler32.INITIAL, out, 0, out.size)
		s.discardBits()
		val adler32 = s.u32be()
		if (chash != adler32) invalidOp("Adler32 doesn't match ${chash.hex} != ${adler32.hex}")
		return out
	}

	fun uncompress(windowBits: Int, i: ByteArray, offset: Int = 0): ByteArray =
		uncompress(windowBits, BitReader(i, offset)).toByteArray()

	fun uncompress(
		windowBits: Int,
		reader: BitReader,
		expectedOutSize: Int = 64,
		out: ByteArrayBuilder.Small = ByteArrayBuilder.Small(expectedOutSize)
	): ByteArrayBuilder.Small = out.apply {
		val temp = TempState()
		val dynTree = HuffmanTree()
		val dynDist = HuffmanTree()
		val sout = SlidingWindowWithOutput(windowBits, out)
		var lastBlock = false
		while (!lastBlock) {
			lastBlock = reader.bit()
			val btype = reader.bits(2)
			if (btype !in 0..2) error("invalid bit")
			if (btype == 0) {
				reader.discardBits()
				val len = reader.u16le()
				val nlen = reader.u16le()
				val nnlen = nlen.inv() and 0xFFFF
				if (len != nnlen) error("Invalid deflate stream: len($len) != ~nlen($nnlen) :: nlen=$nlen")
				sout.putOut(reader.i, reader.alignedBytes(len), len)
			} else {
				val tree: HuffmanTree
				val dist: HuffmanTree
				if (btype == 1) {
					tree = FIXED_TREE
					dist = FIXED_DIST
				} else {
					tree = dynTree
					dist = dynDist
					readDynamicTree(reader, temp, tree, dist)
				}
				while (true) {
					val value = tree.read(reader)
					if (value == 256) break
					if (value < 256) {
						sout.putOut(value.toByte())
					} else {
						val zlenof = value - 257
						val lengthExtra = reader.bits(LEN_EXTRA[zlenof])
						val distanceData = dist.read(reader)
						val distanceExtra = reader.bits(DIST_EXTRA[distanceData])
						val distance = DIST_BASE[distanceData] + distanceExtra
						val length = LEN_BASE[zlenof] + lengthExtra
						sout.getPutCopyOut(distance, length)
					}
				}
			}
		}
	}

	private fun readDynamicTree(reader: BitReader, temp: TempState, l: HuffmanTree, r: HuffmanTree) {
		val hlit = reader.bits(5) + 257
		val hdist = reader.bits(5) + 1
		val hclen = reader.bits(4) + 4
		val codeLenCodeLen = IntArray(19)
		for (i in 0 until hclen) codeLenCodeLen[HCLENPOS[i]] = reader.bits(3)
		//console.info(codeLenCodeLen);
		val codeLen = temp.codeLen.fromLengths(codeLenCodeLen)
		val lengths = IntArray(hlit + hdist)
		var n = 0
		val hlithdist = hlit + hdist
		while (n < hlithdist) {
			val value = codeLen.read(reader)
			if (value !in 0..18) error("Invalid dynamic tree")

			val len = when (value) {
				16 -> reader.bits(2) + 3
				17 -> reader.bits(3) + 3
				18 -> reader.bits(7) + 11
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
		l.fromLengths(lengths, 0, hlit)
		r.fromLengths(lengths, hlit, lengths.size)
	}

	class TempState {
		val codeLen = HuffmanTree()
	}

	open class BitReader(val i: ByteArray, var offset: Int = 0) {
		var bitdata = 0
		var bitsavailable = 0
		var peekbits = 0

		fun discardBits(): BitReader {
			bitdata = 0
			bitsavailable = 0
			offset -= peekbits / 8
			peekbits = 0
			return this
		}

		inline fun drop(bitcount: Int) {
			peekbits -= bitcount
			bitdata = bitdata ushr bitcount
			bitsavailable -= bitcount
		}

		fun ensure(bitcount: Int) {
			while (bitsavailable < bitcount) {
				bitdata = bitdata or (u8() shl bitsavailable)
				bitsavailable += 8
				peekbits += 8
			}
		}

		inline fun peek(bitcount: Int): Int {
			ensure(bitcount)
			return bitdata and ((1 shl bitcount) - 1)
		}

		inline fun bits(bitcount: Int): Int = peek(bitcount).also { drop(bitcount) }

		fun bit(): Boolean = bits(1) != 0

		private fun u8(): Int = i.getOrElse(offset++) { 0 }.toInt() and 0xFF

		fun u16le(): Int {
			val l = u8()
			val h = u8()
			return (h shl 8) or (l)
		}

		fun u32be(): Int {
			val v3 = u8()
			val v2 = u8()
			val v1 = u8()
			val v0 = u8()
			return (v3 shl 24) or (v2 shl 16) or (v1 shl 8) or (v0)
		}

		fun alignedBytes(count: Int): Int {
			discardBits()
			return offset.apply { offset += count }
		}
	}

	class SlidingWindowWithOutput(nbits: Int, val out: ByteArrayBuilder.Small) : SlidingWindow(nbits) {
		//fun getPutCopyOut(distance: Int, length: Int) {
		//	var src = (pos - distance) and mask
		//	var dst = (pos) and mask
		//	for (n in 0 until length) {
		//		val v = data[src]
		//		data[dst] = v
		//		out.append(v)
		//		src = (src + 1) and mask
		//		dst = (dst + 1) and mask
		//	}
		//}
		fun getPutCopyOut(distance: Int, length: Int) {
			out.ensure(length)
			var src = (pos - distance) and mask
			var dst = pos
			val outBytes = out._bytes
			var outPos = out._len
			for (n in 0 until length) {
				val v = data[src]
				data[dst] = v
				outBytes[outPos++] = v
				out.appendUnsafe(v)

				src = (src + 1) and mask
				dst = (dst + 1) and mask
			}
			pos = dst
			out._len = outPos
		}

		fun putOut(bytes: ByteArray, offset: Int, len: Int) {
			out.append(bytes, offset, len)
			putBytes(bytes, offset, len)
		}

		fun putOut(byte: Byte) {
			out.append(byte)
			put(byte.toUnsigned())
		}
	}

	open class SlidingWindow(nbits: Int) {
		val data = ByteArray(1 shl nbits)
		val mask = data.size - 1
		var pos = 0

		fun get(offset: Int): Int = data[(pos - offset) and mask].toInt() and 0xFF
		fun getPut(offset: Int): Int = put(get(offset))
		fun put(value: Int): Int {
			data[pos] = value.toByte()
			pos = (pos + 1) and mask
			return value
		}

		fun putBytes(bytes: ByteArray, offset: Int, len: Int) {
			for (n in 0 until len) put(bytes[offset + n].toUnsigned())
		}
	}

	// @TODO: Compute fast decodings with a lookup table and bit peeking for 9 bits
	class HuffmanTree {
		companion object {
			private const val INVALID_VALUE = -1
			private const val NIL = 1023
			private const val FAST_BITS = 9

			//private const val ENABLE_EXPERIMENTAL_FAST_READ = true
			private const val ENABLE_EXPERIMENTAL_FAST_READ = false
		}

		private val value = IntArray(1024)
		private val left = IntArray(1024)
		private val right = IntArray(1024)

		private var nodeOffset = 0
		private var root: Int = NIL
		private var ncodes: Int = 0

		// Low half-word contains the value, High half-word contains the len
		val FAST_INFO = IntArray(1 shl FAST_BITS) { INVALID_VALUE }

		fun read(reader: BitReader): Int {
			if (ENABLE_EXPERIMENTAL_FAST_READ) {
				val info = FAST_INFO[reader.peek(9)]
				if (info != INVALID_VALUE) {
					val value = info and 0xFFFF
					val bits = (info ushr 16) and 0xFFFF
					reader.drop(bits)
					return value
				}
			}
			return readSlow(reader)
		}

		private fun readSlow(reader: BitReader): Int {
			var node = this.root
			do {
				node = if (reader.bit()) node.right else node.left
			} while (node != NIL && node.value == INVALID_VALUE)
			return node.value
		}

		private fun resetAlloc() {
			nodeOffset = 0
		}

		private fun alloc(value: Int, left: Int, right: Int): Int {
			return (nodeOffset++).apply {
				this@HuffmanTree.value[this] = value
				this@HuffmanTree.left[this] = left
				this@HuffmanTree.right[this] = right
			}
		}

		private fun allocLeaf(value: Int): Int = alloc(value, NIL, NIL)
		private fun allocNode(left: Int, right: Int): Int = alloc(INVALID_VALUE, left, right)

		private inline val Int.value get() = this@HuffmanTree.value[this]
		private inline val Int.left get() = this@HuffmanTree.left[this]
		private inline val Int.right get() = this@HuffmanTree.right[this]

		private val MAX_LEN = 16
		private val COUNTS = IntArray(MAX_LEN + 1)
		private val OFFSETS = IntArray(MAX_LEN + 1)
		private val COFFSET = IntArray(MAX_LEN + 1)
		private val CODES = IntArray(288)

		private val ENCODED_VAL = IntArray(288)
		private val ENCODED_LEN = ByteArray(288)

		fun fromLengths(codeLengths: IntArray, start: Int = 0, end: Int = codeLengths.size): HuffmanTree {
			var oldOffset = 0
			var oldCount = 0
			val ncodes = end - start

			resetAlloc()

			COUNTS.fill(0)

			// Compute the count of codes per length
			for (n in start until end) {
				val codeLen = codeLengths[n]
				if (codeLen !in 0..MAX_LEN) error("Invalid HuffmanTree.codeLengths $codeLen")
				COUNTS[codeLen]++
			}

			// Compute the disposition using the counts per length
			var currentOffset = 0
			for (n in 0 until MAX_LEN) {
				val count = COUNTS[n]
				OFFSETS[n] = currentOffset
				COFFSET[n] = currentOffset
				currentOffset += count
			}

			// Place elements in the computed disposition
			for (n in start until end) {
				val codeLen = codeLengths[n]
				CODES[COFFSET[codeLen]++] = n - start
			}

			for (i in MAX_LEN downTo 1) {
				val newOffset = nodeOffset

				val OFFSET = OFFSETS[i]
				val SIZE = COUNTS[i]
				for (j in 0 until SIZE) allocLeaf(CODES[OFFSET + j])
				for (j in 0 until oldCount step 2) allocNode(oldOffset + j, oldOffset + j + 1)

				oldOffset = newOffset
				oldCount = SIZE + oldCount / 2
				if (oldCount % 2 != 0) error("This canonical code does not represent a Huffman code tree: $oldCount")
			}
			if (oldCount != 2) error("This canonical code does not represent a Huffman code tree")

			this.root = allocNode(nodeOffset - 2, nodeOffset - 1)
			this.ncodes = ncodes

			if (ENABLE_EXPERIMENTAL_FAST_READ) {
				computeFastLookup()
			}

			return this
		}

		private fun computeFastLookup() {
			ENCODED_LEN.fill(0)
			FAST_INFO.fill(INVALID_VALUE)
			computeEncodedValues(root, 0, 0)
			//println("--------------------")
			for (n in 0 until ncodes) {
				val enc = ENCODED_VAL[n]
				val bits = ENCODED_LEN[n].toInt()
				check((enc and 0xFFFF) == enc)
				check((bits and 0xFF) == bits)
				if (bits in 1..FAST_BITS) {
					val remainingBits = FAST_BITS - bits
					val repeat = 1 shl remainingBits
					val info = enc or (bits shl 16)

					//println("n=$n  : enc=$enc : bits=$bits, repeat=$repeat")

					for (j in 0 until repeat) {
						FAST_INFO[enc or (j shl bits)] = info
					}
				}
			}
			//for (fv in FAST_INFO) check(fv != INVALID_VALUE)
		}

		private fun computeEncodedValues(node: Int, encoded: Int, encodedBits: Int) {
			if (node.value == INVALID_VALUE) {
				computeEncodedValues(node.left, encoded, encodedBits + 1)
				computeEncodedValues(node.right, encoded or (1 shl encodedBits), encodedBits + 1)
			} else {
				val nvalue = node.value
				ENCODED_VAL[nvalue] = encoded
				ENCODED_LEN[nvalue] = encodedBits.toByte()
			}
		}
	}
}
