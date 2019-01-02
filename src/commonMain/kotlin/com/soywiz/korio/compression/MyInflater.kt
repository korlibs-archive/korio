package com.soywiz.korio.compression

import com.soywiz.kmem.*
import kotlin.math.*

open class MyDeflate(val windowBits: Int) : MyNewCompressionMethod {
	override fun createCompresor(): StreamProcessor = TODO()
	override fun createDecompresor(): StreamProcessor = MyInflater(windowBits)

	companion object : MyDeflate(15)
}

class MyInflater(val windowBits: Int) : StreamProcessor {
	companion object {
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
	}

	private lateinit var sequence: Iterator<StreamProcessor.Status>
	private var reachedEnd: Boolean = false
	private val input: ByteArrayDeque = ByteArrayDeque()
	private val bits: BitReader = BitReader(input)
	private val output: ByteArrayDeque = ByteArrayDeque()
	private val ring: SlidingWindowWithOutput = SlidingWindowWithOutput(windowBits, output)

	override val availableInput: Int get() = input.availableWrite
	override val availableOutput: Int get() = output.availableRead

	override fun reset() {
		input.clear()
		output.clear()
		bits.discardBits()
		ring.clear()
		reachedEnd = false
		sequence = internalProcess().iterator()
	}

	init {
		reset()
	}

	override fun addInput(data: ByteArray, offset: Int, len: Int): Int = input.writeBytes(data, offset, len)
	override fun inputEod() = run { reachedEnd = true }
	override fun readOutput(data: ByteArray, offset: Int, len: Int): Int = output.readBytes(data, offset, len)

	override fun process(): StreamProcessor.Status {
		//println("process[0]")
		return if (sequence.hasNext()) {
			//println("process[1]")
			sequence.next()
		} else {
			//println("process[2]")
			StreamProcessor.Status.FINISHED
		}
	}

	private suspend inline fun SequenceScope<StreamProcessor.Status>.ensureBytes(count: Int = 32) {
		while (!reachedEnd && input.availableRead < count) {
			yield(StreamProcessor.Status.NEED_INPUT)
		}
	}

	private fun internalProcess(): Sequence<StreamProcessor.Status> = sequence {
		val temp = ByteArray(1024)
		val codeLenCodeLen = IntArray(20)
		val lengths = IntArray(350)
		val tempCodeLen = HuffmanTree()
		val dynTree = HuffmanTree()
		val dynDist = HuffmanTree()
		var lastBlock = false
		val bits = this@MyInflater.bits
		while (!lastBlock) {
			ensureBytes()
			lastBlock = bits.bit()
			val btype = bits.bits(2)
			//println("lastBlock=$lastBlock, btype=$btype")
			if (btype !in 0..2) error("invalid bit")
			if (btype == 0) {
				bits.discardBits()
				val len = bits.u16le()
				val nlen = bits.u16le()
				val nnlen = nlen.inv() and 0xFFFF
				if (len != nnlen) error("Invalid deflate stream: len($len) != ~nlen($nnlen) :: nlen=$nlen")

				var pending = len
				while (pending > 0) {
					val read = bits.readBytesAligned(temp, 0, min(pending, temp.size))
					if (read <= 0) yield(StreamProcessor.Status.NEED_INPUT)
					ring.putOut(temp, 0, read)
					pending -= read
				}
			} else {
				val tree: HuffmanTree
				val dist: HuffmanTree
				if (btype == 1) {
					tree = FIXED_TREE
					dist = FIXED_DIST
				} else {
					tree = dynTree
					dist = dynDist
					val hlit = bits.bits(5) + 257
					val hdist = bits.bits(5) + 1
					val hclen = bits.bits(4) + 4
					ensureBytes(hclen / 2)
					for (i in 0 until hclen) codeLenCodeLen[HCLENPOS[i]] = bits.bits(3)
					//console.info(codeLenCodeLen);
					val codeLen = tempCodeLen.fromLengths(codeLenCodeLen)
					var n = 0
					val hlithdist = hlit + hdist
					while (n < hlithdist) {
						ensureBytes()
						val value = codeLen.read(bits)
						if (value !in 0..18) error("Invalid dynamic tree")

						val len = when (value) {
							16 -> bits.bits(2) + 3
							17 -> bits.bits(3) + 3
							18 -> bits.bits(7) + 11
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
					tree.fromLengths(lengths, 0, hlit)
					dist.fromLengths(lengths, hlit, lengths.size)
				}
				while (true) {
					ensureBytes()
					val value = tree.read(bits)
					//println("value=$value")
					if (value == 256) break
					if (value < 256) {
						ring.putOut(value.toByte())
					} else {
						val zlenof = value - 257
						val lengthExtra = bits.bits(LEN_EXTRA[zlenof])
						val distanceData = dist.read(bits)
						val distanceExtra = bits.bits(DIST_EXTRA[distanceData])
						val distance = DIST_BASE[distanceData] + distanceExtra
						val length = LEN_BASE[zlenof] + lengthExtra
						ring.getPutCopyOut(distance, length)
					}
				}
			}
		}
		//println("DONE!")
	}

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

	class SlidingWindowWithOutput(nbits: Int, val out: ByteWriter) {
		private val data = ByteArray(1 shl nbits)
		private val mask = data.size - 1
		private var pos = 0

		fun clear() {
			pos = 0
		}

		private fun get(offset: Int): Int = data[(pos - offset) and mask].toInt() and 0xFF
		private fun getPut(offset: Int): Int = put(get(offset))
		private fun put(value: Int): Int {
			data[pos] = value.toByte()
			pos = (pos + 1) and mask
			return value
		}

		private fun putBytes(bytes: ByteArray, offset: Int, len: Int) {
			for (n in 0 until len) put(bytes[offset + n].unsigned)
		}

		fun getPutCopyOut(distance: Int, length: Int) {
			//out.ensure(length)
			//out.size = max(out.size, length)
			//println("distance=$distance, length=$length")
			var src = (pos - distance) and mask
			var dst = pos
			for (n in 0 until length) {
				val v = data[src]
				data[dst] = v
				out.writeByte(v.unsigned)

				src = (src + 1) and mask
				dst = (dst + 1) and mask
			}
			pos = dst
		}

		fun putOut(bytes: ByteArray, offset: Int, len: Int) {
			out.writeBytes(bytes, offset, len)
			putBytes(bytes, offset, len)
		}

		fun putOut(byte: Byte) {
			out.writeByte(byte.unsigned)
			put(byte.unsigned)
		}
	}
}