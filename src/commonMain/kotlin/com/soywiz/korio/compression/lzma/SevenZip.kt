package com.soywiz.korio.compression.lzma

import com.soywiz.korio.stream.*
import com.soywiz.korio.util.checksum.*
import kotlin.math.*

object SevenZip {

	interface ICodeProgress {
		fun SetProgress(inSize: Long, outSize: Long)
	}

	class BitTreeEncoder(internal var NumBitLevels: Int) {
		internal val Models: ShortArray = ShortArray(1 shl NumBitLevels)

		fun Init() {
			RangeDecoder.InitBitModels(Models)
		}

		fun Encode(rangeEncoder: RangeEncoder, symbol: Int) {
			var m = 1
			var bitIndex = NumBitLevels
			while (bitIndex != 0) {
				bitIndex--
				val bit = symbol.ushr(bitIndex) and 1
				rangeEncoder.Encode(Models, m, bit)
				m = m shl 1 or bit
			}
		}

		fun ReverseEncode(rangeEncoder: RangeEncoder, symbol: Int) {
			var symbol = symbol
			var m = 1
			for (i in 0 until NumBitLevels) {
				val bit = symbol and 1
				rangeEncoder.Encode(Models, m, bit)
				m = m shl 1 or bit
				symbol = symbol shr 1
			}
		}

		fun GetPrice(symbol: Int): Int {
			var price = 0
			var m = 1
			var bitIndex = NumBitLevels
			while (bitIndex != 0) {
				bitIndex--
				val bit = symbol.ushr(bitIndex) and 1
				price += RangeEncoder.GetPrice(
					Models[m].toInt(),
					bit
				)
				m = (m shl 1) + bit
			}
			return price
		}

		fun ReverseGetPrice(symbol: Int): Int {
			var symbol = symbol
			var price = 0
			var m = 1
			for (i in NumBitLevels downTo 1) {
				val bit = symbol and 1
				symbol = symbol ushr 1
				price += RangeEncoder.GetPrice(
					Models[m].toInt(),
					bit
				)
				m = m shl 1 or bit
			}
			return price
		}

		companion object {

			fun ReverseGetPrice(
				Models: ShortArray, startIndex: Int,
				NumBitLevels: Int, symbol: Int
			): Int {
				var symbol = symbol
				var price = 0
				var m = 1
				for (i in NumBitLevels downTo 1) {
					val bit = symbol and 1
					symbol = symbol ushr 1
					price += RangeEncoder.GetPrice(
						Models[startIndex + m].toInt(),
						bit
					)
					m = m shl 1 or bit
				}
				return price
			}

			fun ReverseEncode(
				Models: ShortArray, startIndex: Int,
				rangeEncoder: RangeEncoder, NumBitLevels: Int, symbol: Int
			) {
				var symbol = symbol
				var m = 1
				for (i in 0 until NumBitLevels) {
					val bit = symbol and 1
					rangeEncoder.Encode(Models, startIndex + m, bit)
					m = m shl 1 or bit
					symbol = symbol shr 1
				}
			}
		}
	}

	class BitTreeDecoder(internal var NumBitLevels: Int) {
		internal val Models: ShortArray = ShortArray(1 shl NumBitLevels)

		fun Init() {
			RangeDecoder.InitBitModels(Models)
		}

		fun Decode(rangeDecoder: RangeDecoder): Int {
			var m = 1
			for (bitIndex in NumBitLevels downTo 1)
				m = (m shl 1) + rangeDecoder.DecodeBit(Models, m)
			return m - (1 shl NumBitLevels)
		}

		fun ReverseDecode(rangeDecoder: RangeDecoder): Int {
			var m = 1
			var symbol = 0
			for (bitIndex in 0 until NumBitLevels) {
				val bit = rangeDecoder.DecodeBit(Models, m)
				m = m shl 1
				m += bit
				symbol = symbol or (bit shl bitIndex)
			}
			return symbol
		}

		companion object {

			fun ReverseDecode(
				Models: ShortArray, startIndex: Int,
				rangeDecoder: RangeDecoder, NumBitLevels: Int
			): Int {
				var m = 1
				var symbol = 0
				for (bitIndex in 0 until NumBitLevels) {
					val bit = rangeDecoder.DecodeBit(Models, startIndex + m)
					m = m shl 1
					m += bit
					symbol = symbol or (bit shl bitIndex)
				}
				return symbol
			}
		}
	}

	class RangeDecoder {

		internal var Range: Int = 0
		internal var Code: Int = 0

		internal var Stream: SyncInputStream? = null

		fun SetStream(stream: SyncInputStream) {
			Stream = stream
		}

		fun ReleaseStream() {
			Stream = null
		}

		fun Init() {
			Code = 0
			Range = -1
			for (i in 0..4)
				Code = Code shl 8 or Stream!!.read()
		}

		fun DecodeDirectBits(numTotalBits: Int): Int {
			var result = 0
			for (i in numTotalBits downTo 1) {
				Range = Range ushr 1
				val t = (Code - Range).ushr(31)
				Code -= Range and t - 1
				result = result shl 1 or 1 - t

				if (Range and kTopMask == 0) {
					Code = Code shl 8 or Stream!!.read()
					Range = Range shl 8
				}
			}
			return result
		}

		fun DecodeBit(probs: ShortArray, index: Int): Int {
			val prob = probs[index].toInt()
			val newBound = Range.ushr(kNumBitModelTotalBits) * prob
			if (Code xor -0x80000000 < newBound xor -0x80000000) {
				Range = newBound
				probs[index] = (prob + (kBitModelTotal - prob).ushr(
					kNumMoveBits
				)).toShort()
				if (Range and kTopMask == 0) {
					Code = Code shl 8 or Stream!!.read()
					Range = Range shl 8
				}
				return 0
			} else {
				Range -= newBound
				Code -= newBound
				probs[index] = (prob - prob.ushr(kNumMoveBits)).toShort()
				if (Range and kTopMask == 0) {
					Code = Code shl 8 or Stream!!.read()
					Range = Range shl 8
				}
				return 1
			}
		}

		companion object {
			internal const val kTopMask = ((1 shl 24) - 1).inv()

			internal const val kNumBitModelTotalBits = 11
			internal const val kBitModelTotal = 1 shl kNumBitModelTotalBits
			internal const val kNumMoveBits = 5

			fun InitBitModels(probs: ShortArray) {
				for (i in probs.indices)
					probs[i] = kBitModelTotal.ushr(1).toShort()
			}
		}
	}

	class RangeEncoder {

		internal var Stream: SyncOutputStream? = null

		internal var Low: Long = 0
		internal var Range: Int = 0
		internal var _cacheSize: Int = 0
		internal var _cache: Int = 0

		internal var _position: Long = 0

		fun SetStream(stream: SyncOutputStream) {
			Stream = stream
		}

		fun ReleaseStream() {
			Stream = null
		}

		fun Init() {
			_position = 0
			Low = 0
			Range = -1
			_cacheSize = 1
			_cache = 0
		}

		fun FlushData() {
			for (i in 0..4)
				ShiftLow()
		}

		fun FlushStream() {
			Stream!!.flush()
		}

		fun ShiftLow() {
			val LowHi = Low.ushr(32).toInt()
			if (LowHi != 0 || Low < 0xFF000000L) {
				_position += _cacheSize.toLong()
				var temp = _cache
				do {
					Stream!!.write8(temp + LowHi)
					temp = 0xFF
				} while (--_cacheSize != 0)
				_cache = Low.toInt().ushr(24)
			}
			_cacheSize++
			Low = Low and 0xFFFFFF shl 8
		}

		fun EncodeDirectBits(v: Int, numTotalBits: Int) {
			for (i in numTotalBits - 1 downTo 0) {
				Range = Range ushr 1
				if (v.ushr(i) and 1 == 1)
					Low += Range.toLong()
				if (Range and kTopMask == 0) {
					Range = Range shl 8
					ShiftLow()
				}
			}
		}


		fun GetProcessedSizeAdd(): Long {
			return _cacheSize.toLong() + _position + 4
		}

		fun Encode(probs: ShortArray, index: Int, symbol: Int) {
			val prob = probs[index].toInt()
			val newBound = Range.ushr(kNumBitModelTotalBits) * prob
			if (symbol == 0) {
				Range = newBound
				probs[index] = (prob + (kBitModelTotal - prob).ushr(
					kNumMoveBits
				)).toShort()
			} else {
				Low += newBound and 0xFFFFFFFFL.toInt()
				Range -= newBound
				probs[index] = (prob - prob.ushr(kNumMoveBits)).toShort()
			}
			if (Range and kTopMask == 0) {
				Range = Range shl 8
				ShiftLow()
			}
		}

		companion object {
			internal const val kTopMask = ((1 shl 24) - 1).inv()

			internal const val kNumBitModelTotalBits = 11
			internal const val kBitModelTotal = 1 shl kNumBitModelTotalBits
			internal const val kNumMoveBits = 5
			internal const val kNumMoveReducingBits = 2
			const val kNumBitPriceShiftBits = 6

			fun InitBitModels(probs: ShortArray) {
				for (i in probs.indices)
					probs[i] = kBitModelTotal.ushr(1).toShort()
			}

			private val ProbPrices = IntArray(
				kBitModelTotal.ushr(
					kNumMoveReducingBits
				)
			)

			init {
				val kNumBits = kNumBitModelTotalBits - kNumMoveReducingBits
				for (i in kNumBits - 1 downTo 0) {
					val start = 1 shl kNumBits - i - 1
					val end = 1 shl kNumBits - i
					for (j in start until end)
						ProbPrices[j] = (i shl kNumBitPriceShiftBits) +
								(end - j shl kNumBitPriceShiftBits).ushr(kNumBits - i - 1)
				}
			}

			fun GetPrice(Prob: Int, symbol: Int): Int {
				return ProbPrices[(Prob - symbol xor -symbol and kBitModelTotal - 1).ushr(
					kNumMoveReducingBits
				)]
			}

			fun GetPrice0(Prob: Int): Int {
				return ProbPrices[Prob.ushr(
					kNumMoveReducingBits
				)]
			}

			fun GetPrice1(Prob: Int): Int {
				return ProbPrices[(kBitModelTotal - Prob).ushr(
					kNumMoveReducingBits
				)]
			}
		}
	}

	object LzmaBase {
		const val kNumRepDistances = 4
		const val kNumStates = 12

		const val kNumPosSlotBits = 6
		const val kDicLogSizeMin = 0
		// public static final int kDicLogSizeMax = 28;
		// public static final int kDistTableSizeMax = kDicLogSizeMax * 2;

		const val kNumLenToPosStatesBits = 2 // it's for speed optimization
		const val kNumLenToPosStates = 1 shl kNumLenToPosStatesBits

		const val kMatchMinLen = 2

		const val kNumAlignBits = 4
		const val kAlignTableSize = 1 shl kNumAlignBits
		const val kAlignMask = kAlignTableSize - 1

		const val kStartPosModelIndex = 4
		const val kEndPosModelIndex = 14
		const val kNumPosModels = kEndPosModelIndex - kStartPosModelIndex

		const val kNumFullDistances = 1 shl kEndPosModelIndex / 2

		const val kNumLitPosStatesBitsEncodingMax = 4
		const val kNumLitContextBitsMax = 8

		const val kNumPosStatesBitsMax = 4
		const val kNumPosStatesMax = 1 shl kNumPosStatesBitsMax
		const val kNumPosStatesBitsEncodingMax = 4
		const val kNumPosStatesEncodingMax = 1 shl kNumPosStatesBitsEncodingMax

		const val kNumLowLenBits = 3
		const val kNumMidLenBits = 3
		const val kNumHighLenBits = 8
		const val kNumLowLenSymbols = 1 shl kNumLowLenBits
		const val kNumMidLenSymbols = 1 shl kNumMidLenBits
		const val kNumLenSymbols = kNumLowLenSymbols + kNumMidLenSymbols + (1 shl kNumHighLenBits)
		const val kMatchMaxLen = kMatchMinLen + kNumLenSymbols - 1

		fun StateInit(): Int = 0

		fun StateUpdateChar(index: Int): Int = when {
			index < 4 -> 0
			index < 10 -> index - 3
			else -> index - 6
		}

		fun StateUpdateMatch(index: Int): Int = if (index < 7) 7 else 10
		fun StateUpdateRep(index: Int): Int = if (index < 7) 8 else 11
		fun StateUpdateShortRep(index: Int): Int = if (index < 7) 9 else 11
		fun StateIsCharState(index: Int): Boolean = index < 7

		fun GetLenToPosState(len: Int): Int {
			var len = len
			len -= kMatchMinLen
			return if (len < kNumLenToPosStates) len else kNumLenToPosStates - 1
		}
	}

	class LzmaDecoder {

		internal var m_OutWindow = LzOutWindow()
		internal var m_RangeDecoder = RangeDecoder()

		internal var m_IsMatchDecoders = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)
		internal var m_IsRepDecoders = ShortArray(LzmaBase.kNumStates)
		internal var m_IsRepG0Decoders = ShortArray(LzmaBase.kNumStates)
		internal var m_IsRepG1Decoders = ShortArray(LzmaBase.kNumStates)
		internal var m_IsRepG2Decoders = ShortArray(LzmaBase.kNumStates)
		internal var m_IsRep0LongDecoders = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)

		internal var m_PosSlotDecoder = arrayOfNulls<BitTreeDecoder>(
			LzmaBase.kNumLenToPosStates
		)
		internal var m_PosDecoders = ShortArray(LzmaBase.kNumFullDistances - LzmaBase.kEndPosModelIndex)

		internal var m_PosAlignDecoder =
			BitTreeDecoder(LzmaBase.kNumAlignBits)

		internal var m_LenDecoder = LenDecoder()
		internal var m_RepLenDecoder = LenDecoder()

		internal var m_LiteralDecoder = LiteralDecoder()

		internal var m_DictionarySize = -1
		internal var m_DictionarySizeCheck = -1

		internal var m_PosStateMask: Int = 0

		internal inner class LenDecoder {
			var m_Choice = ShortArray(2)
			var m_LowCoder = arrayOfNulls<BitTreeDecoder>(
				LzmaBase.kNumPosStatesMax
			)
			var m_MidCoder = arrayOfNulls<BitTreeDecoder>(
				LzmaBase.kNumPosStatesMax
			)
			var m_HighCoder =
				BitTreeDecoder(LzmaBase.kNumHighLenBits)
			var m_NumPosStates = 0

			fun Create(numPosStates: Int) {
				while (m_NumPosStates < numPosStates) {
					m_LowCoder[m_NumPosStates] =
							BitTreeDecoder(LzmaBase.kNumLowLenBits)
					m_MidCoder[m_NumPosStates] =
							BitTreeDecoder(LzmaBase.kNumMidLenBits)
					m_NumPosStates++
				}
			}

			fun Init() {
				RangeDecoder.InitBitModels(
					m_Choice
				)
				for (posState in 0 until m_NumPosStates) {
					m_LowCoder[posState]!!.Init()
					m_MidCoder[posState]!!.Init()
				}
				m_HighCoder.Init()
			}

			fun Decode(
				rangeDecoder: RangeDecoder,
				posState: Int
			): Int {
				if (rangeDecoder.DecodeBit(m_Choice, 0) == 0)
					return m_LowCoder[posState]!!.Decode(rangeDecoder)
				var symbol = LzmaBase.kNumLowLenSymbols
				if (rangeDecoder.DecodeBit(m_Choice, 1) == 0)
					symbol += m_MidCoder[posState]!!.Decode(rangeDecoder)
				else
					symbol += LzmaBase.kNumMidLenSymbols + m_HighCoder.Decode(rangeDecoder)
				return symbol
			}
		}

		internal inner class LiteralDecoder {

			var m_Coders: Array<Decoder2>? = null
			var m_NumPrevBits: Int = 0
			var m_NumPosBits: Int = 0
			var m_PosMask: Int = 0

			internal inner class Decoder2 {
				var m_Decoders = ShortArray(0x300)

				fun Init() {
					RangeDecoder.InitBitModels(
						m_Decoders
					)
				}

				fun DecodeNormal(rangeDecoder: RangeDecoder): Byte {
					var symbol = 1
					do
						symbol = symbol shl 1 or rangeDecoder.DecodeBit(m_Decoders, symbol)
					while (symbol < 0x100)
					return symbol.toByte()
				}

				fun DecodeWithMatchByte(
					rangeDecoder: RangeDecoder,
					matchByte: Byte
				): Byte {
					var matchByte = matchByte
					var symbol = 1
					do {
						val matchBit = (matchByte shr 7) and 1
						matchByte = ((matchByte shl 1).toByte())
						val bit = rangeDecoder.DecodeBit(m_Decoders, (1 + matchBit shl 8) + symbol)
						symbol = symbol shl 1 or bit
						if (matchBit != bit) {
							while (symbol < 0x100)
								symbol = symbol shl 1 or rangeDecoder.DecodeBit(m_Decoders, symbol)
							break
						}
					} while (symbol < 0x100)
					return symbol.toByte()
				}
			}

			fun Create(numPosBits: Int, numPrevBits: Int) {
				if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits)
					return
				m_NumPosBits = numPosBits
				m_PosMask = (1 shl numPosBits) - 1
				m_NumPrevBits = numPrevBits
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				m_Coders = Array(numStates) { Decoder2() }
			}

			fun Init() {
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				for (i in 0 until numStates)
					m_Coders!![i].Init()
			}

			fun GetDecoder(pos: Int, prevByte: Byte): Decoder2 {
				return m_Coders!![(pos and m_PosMask shl m_NumPrevBits) + (prevByte and 0xFF).ushr(8 - m_NumPrevBits)]
			}
		}

		init {
			for (i in 0 until LzmaBase.kNumLenToPosStates)
				m_PosSlotDecoder[i] =
						BitTreeDecoder(LzmaBase.kNumPosSlotBits)
		}

		internal fun SetDictionarySize(dictionarySize: Int): Boolean {
			if (dictionarySize < 0)
				return false
			if (m_DictionarySize != dictionarySize) {
				m_DictionarySize = dictionarySize
				m_DictionarySizeCheck = max(m_DictionarySize, 1)
				m_OutWindow.Create(max(m_DictionarySizeCheck, 1 shl 12))
			}
			return true
		}

		internal fun SetLcLpPb(lc: Int, lp: Int, pb: Int): Boolean {
			if (lc > LzmaBase.kNumLitContextBitsMax || lp > 4 || pb > LzmaBase.kNumPosStatesBitsMax)
				return false
			m_LiteralDecoder.Create(lp, lc)
			val numPosStates = 1 shl pb
			m_LenDecoder.Create(numPosStates)
			m_RepLenDecoder.Create(numPosStates)
			m_PosStateMask = numPosStates - 1
			return true
		}

		internal fun Init() {
			m_OutWindow.Init(false)

			RangeDecoder.InitBitModels(m_IsMatchDecoders)
			RangeDecoder.InitBitModels(m_IsRep0LongDecoders)
			RangeDecoder.InitBitModels(m_IsRepDecoders)
			RangeDecoder.InitBitModels(m_IsRepG0Decoders)
			RangeDecoder.InitBitModels(m_IsRepG1Decoders)
			RangeDecoder.InitBitModels(m_IsRepG2Decoders)
			RangeDecoder.InitBitModels(m_PosDecoders)

			m_LiteralDecoder.Init()
			for (i in 0 until LzmaBase.kNumLenToPosStates) {
				m_PosSlotDecoder[i]!!.Init()
			}
			m_LenDecoder.Init()
			m_RepLenDecoder.Init()
			m_PosAlignDecoder.Init()
			m_RangeDecoder.Init()
		}

		fun Code(
			inStream: SyncInputStream, outStream: SyncOutputStream,
			outSize: Long
		): Boolean {
			m_RangeDecoder.SetStream(inStream)
			m_OutWindow.SetStream(outStream)
			Init()

			var state = LzmaBase.StateInit()
			var rep0 = 0
			var rep1 = 0
			var rep2 = 0
			var rep3 = 0

			var nowPos64: Long = 0
			var prevByte: Byte = 0
			while (outSize < 0 || nowPos64 < outSize) {
				val posState = nowPos64.toInt() and m_PosStateMask
				if (m_RangeDecoder.DecodeBit(
						m_IsMatchDecoders,
						(state shl LzmaBase.kNumPosStatesBitsMax) + posState
					) == 0
				) {
					val decoder2 = m_LiteralDecoder.GetDecoder(nowPos64.toInt(), prevByte)
					if (!LzmaBase.StateIsCharState(state))
						prevByte = decoder2.DecodeWithMatchByte(m_RangeDecoder, m_OutWindow.GetByte(rep0))
					else
						prevByte = decoder2.DecodeNormal(m_RangeDecoder)
					m_OutWindow.PutByte(prevByte)
					state = LzmaBase.StateUpdateChar(state)
					nowPos64++
				} else {
					var len: Int
					if (m_RangeDecoder.DecodeBit(m_IsRepDecoders, state) == 1) {
						len = 0
						if (m_RangeDecoder.DecodeBit(m_IsRepG0Decoders, state) == 0) {
							if (m_RangeDecoder.DecodeBit(
									m_IsRep0LongDecoders,
									(state shl LzmaBase.kNumPosStatesBitsMax) + posState
								) == 0
							) {
								state = LzmaBase.StateUpdateShortRep(
									state
								)
								len = 1
							}
						} else {
							val distance: Int
							if (m_RangeDecoder.DecodeBit(m_IsRepG1Decoders, state) == 0)
								distance = rep1
							else {
								if (m_RangeDecoder.DecodeBit(m_IsRepG2Decoders, state) == 0)
									distance = rep2
								else {
									distance = rep3
									rep3 = rep2
								}
								rep2 = rep1
							}
							rep1 = rep0
							rep0 = distance
						}
						if (len == 0) {
							len = m_RepLenDecoder.Decode(m_RangeDecoder, posState) +
									LzmaBase.kMatchMinLen
							state = LzmaBase.StateUpdateRep(state)
						}
					} else {
						rep3 = rep2
						rep2 = rep1
						rep1 = rep0
						len = LzmaBase.kMatchMinLen + m_LenDecoder.Decode(m_RangeDecoder, posState)
						state = LzmaBase.StateUpdateMatch(state)
						val posSlot = m_PosSlotDecoder[LzmaBase.GetLenToPosState(
							len
						)]!!.Decode(m_RangeDecoder)
						if (posSlot >= LzmaBase.kStartPosModelIndex) {
							val numDirectBits = (posSlot shr 1) - 1
							rep0 = 2 or (posSlot and 1) shl numDirectBits
							if (posSlot < LzmaBase.kEndPosModelIndex)
								rep0 += BitTreeDecoder.ReverseDecode(
									m_PosDecoders,
									rep0 - posSlot - 1, m_RangeDecoder, numDirectBits
								)
							else {
								rep0 += m_RangeDecoder.DecodeDirectBits(
									numDirectBits - LzmaBase.kNumAlignBits
								) shl LzmaBase.kNumAlignBits
								rep0 += m_PosAlignDecoder.ReverseDecode(m_RangeDecoder)
								if (rep0 < 0) {
									if (rep0 == -1)
										break
									return false
								}
							}
						} else
							rep0 = posSlot
					}
					if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck) {
						// m_OutWindow.Flush();
						return false
					}
					m_OutWindow.CopyBlock(rep0, len)
					nowPos64 += len.toLong()
					prevByte = m_OutWindow.GetByte(0)
				}
			}
			m_OutWindow.Flush()
			m_OutWindow.ReleaseStream()
			m_RangeDecoder.ReleaseStream()
			return true
		}

		fun SetDecoderProperties(properties: ByteArray): Boolean {
			if (properties.size < 5)
				return false
			val `val` = properties[0] and 0xFF
			val lc = `val` % 9
			val remainder = `val` / 9
			val lp = remainder % 5
			val pb = remainder / 5
			var dictionarySize = 0
			for (i in 0..3)
				dictionarySize += properties[1 + i].toInt() and 0xFF shl i * 8
			return if (!SetLcLpPb(lc, lp, pb)) false else SetDictionarySize(dictionarySize)
		}
	}

	class LzmaEncoder {

		internal var _state = LzmaBase.StateInit()
		internal var _previousByte: Byte = 0
		internal var _repDistances = IntArray(LzmaBase.kNumRepDistances)
		internal var _optimum = Array<Optimal>(kNumOpts) { Optimal() }

		internal var _matchFinder: LzBinTree? = null
		internal var _rangeEncoder = RangeEncoder()

		internal var _isMatch = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)
		internal var _isRep = ShortArray(LzmaBase.kNumStates)
		internal var _isRepG0 = ShortArray(LzmaBase.kNumStates)
		internal var _isRepG1 = ShortArray(LzmaBase.kNumStates)
		internal var _isRepG2 = ShortArray(LzmaBase.kNumStates)
		internal var _isRep0Long = ShortArray(LzmaBase.kNumStates shl LzmaBase.kNumPosStatesBitsMax)

		internal var _posSlotEncoder =
			Array<BitTreeEncoder>(LzmaBase.kNumLenToPosStates) {
				BitTreeEncoder(
					LzmaBase.kNumPosSlotBits
				)
			} // kNumPosSlotBits


		internal var _posEncoders = ShortArray(LzmaBase.kNumFullDistances - LzmaBase.kEndPosModelIndex)
		internal var _posAlignEncoder =
			BitTreeEncoder(LzmaBase.kNumAlignBits)

		internal var _lenEncoder = LenPriceTableEncoder()
		internal var _repMatchLenEncoder = LenPriceTableEncoder()

		internal var _literalEncoder = LiteralEncoder()

		internal var _matchDistances = IntArray(LzmaBase.kMatchMaxLen * 2 + 2)

		internal var _numFastBytes =
			kNumFastBytesDefault
		internal var _longestMatchLength: Int = 0
		internal var _numDistancePairs: Int = 0

		internal var _additionalOffset: Int = 0

		internal var _optimumEndIndex: Int = 0
		internal var _optimumCurrentIndex: Int = 0

		internal var _longestMatchWasFound: Boolean = false

		internal var _posSlotPrices = IntArray(1 shl LzmaBase.kNumPosSlotBits + LzmaBase.kNumLenToPosStatesBits)
		internal var _distancesPrices = IntArray(LzmaBase.kNumFullDistances shl LzmaBase.kNumLenToPosStatesBits)
		internal var _alignPrices = IntArray(LzmaBase.kAlignTableSize)
		internal var _alignPriceCount: Int = 0

		internal var _distTableSize = kDefaultDictionaryLogSize * 2

		internal var _posStateBits = 2
		internal var _posStateMask = 4 - 1
		internal var _numLiteralPosStateBits = 0
		internal var _numLiteralContextBits = 3

		internal var _dictionarySize = 1 shl kDefaultDictionaryLogSize
		internal var _dictionarySizePrev = -1
		internal var _numFastBytesPrev = -1

		internal var nowPos64: Long = 0
		internal var _finished: Boolean = false
		internal var _inStream: SyncInputStream? = null

		internal var _matchFinderType =
			EMatchFinderTypeBT4
		internal var _writeEndMark = false

		internal var _needReleaseMFStream = false

		internal var reps = IntArray(LzmaBase.kNumRepDistances)
		internal var repLens = IntArray(LzmaBase.kNumRepDistances)
		internal var backRes: Int = 0

		internal var processedInSize = LongArray(1)
		internal var processedOutSize = LongArray(1)
		internal var finished = BooleanArray(1)
		internal var properties = ByteArray(kPropSize)

		internal var tempPrices = IntArray(LzmaBase.kNumFullDistances)
		internal var _matchPriceCount: Int = 0

		internal fun BaseInit() {
			_state = LzmaBase.StateInit()
			_previousByte = 0
			for (i in 0 until LzmaBase.kNumRepDistances)
				_repDistances[i] = 0
		}

		internal inner class LiteralEncoder {

			var m_Coders: Array<Encoder2>? = null
			var m_NumPrevBits: Int = 0
			var m_NumPosBits: Int = 0
			var m_PosMask: Int = 0

			internal inner class Encoder2 {
				var m_Encoders = ShortArray(0x300)

				fun Init() {
					RangeEncoder.InitBitModels(m_Encoders)
				}

				fun Encode(
					rangeEncoder: RangeEncoder,
					symbol: Byte
				) {
					var context = 1
					for (i in 7 downTo 0) {
						val bit = (symbol shr i) and 1
						rangeEncoder.Encode(m_Encoders, context, bit)
						context = context shl 1 or bit
					}
				}

				fun EncodeMatched(
					rangeEncoder: RangeEncoder,
					matchByte: Byte,
					symbol: Byte
				) {
					var context = 1
					var same = true
					for (i in 7 downTo 0) {
						val bit = symbol shr i and 1
						var state = context
						if (same) {
							val matchBit = matchByte shr i and 1
							state += 1 + matchBit shl 8
							same = matchBit == bit
						}
						rangeEncoder.Encode(m_Encoders, state, bit)
						context = context shl 1 or bit
					}
				}

				fun GetPrice(matchMode: Boolean, matchByte: Byte, symbol: Byte): Int {
					var price = 0
					var context = 1
					var i = 7
					if (matchMode) {
						while (i >= 0) {
							val matchBit = matchByte shr i and 1
							val bit = symbol shr i and 1
							price += RangeEncoder.GetPrice(
								m_Encoders[(1 + matchBit shl 8) + context].toInt(),
								bit
							)
							context = context shl 1 or bit
							if (matchBit != bit) {
								i--
								break
							}
							i--
						}
					}
					while (i >= 0) {
						val bit = symbol shr i and 1
						price += RangeEncoder.GetPrice(
							m_Encoders[context].toInt(),
							bit
						)
						context = context shl 1 or bit
						i--
					}
					return price
				}
			}

			fun Create(numPosBits: Int, numPrevBits: Int) {
				if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits)
					return
				m_NumPosBits = numPosBits
				m_PosMask = (1 shl numPosBits) - 1
				m_NumPrevBits = numPrevBits
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				m_Coders = Array(numStates) { Encoder2() }
			}

			fun Init() {
				val numStates = 1 shl m_NumPrevBits + m_NumPosBits
				for (i in 0 until numStates)
					m_Coders!![i].Init()
			}

			fun GetSubCoder(pos: Int, prevByte: Byte): Encoder2 =
				m_Coders!![(pos and m_PosMask shl m_NumPrevBits) + (prevByte and 0xFF).ushr(8 - m_NumPrevBits)]
		}

		internal open inner class LenEncoder {
			var _choice = ShortArray(2)
			var _lowCoder = arrayOfNulls<BitTreeEncoder>(LzmaBase.kNumPosStatesEncodingMax)
			var _midCoder = arrayOfNulls<BitTreeEncoder>(LzmaBase.kNumPosStatesEncodingMax)
			var _highCoder = BitTreeEncoder(LzmaBase.kNumHighLenBits)

			init {
				for (posState in 0 until LzmaBase.kNumPosStatesEncodingMax) {
					_lowCoder[posState] =
							BitTreeEncoder(LzmaBase.kNumLowLenBits)
					_midCoder[posState] =
							BitTreeEncoder(LzmaBase.kNumMidLenBits)
				}
			}

			fun Init(numPosStates: Int) {
				RangeEncoder.InitBitModels(_choice)

				for (posState in 0 until numPosStates) {
					_lowCoder[posState]!!.Init()
					_midCoder[posState]!!.Init()
				}
				_highCoder.Init()
			}

			open fun Encode(
				rangeEncoder: RangeEncoder,
				symbol: Int,
				posState: Int
			) {
				var sym = symbol
				if (sym < LzmaBase.kNumLowLenSymbols) {
					rangeEncoder.Encode(_choice, 0, 0)
					_lowCoder[posState]!!.Encode(rangeEncoder, sym)
				} else {
					sym -= LzmaBase.kNumLowLenSymbols
					rangeEncoder.Encode(_choice, 0, 1)
					if (sym < LzmaBase.kNumMidLenSymbols) {
						rangeEncoder.Encode(_choice, 1, 0)
						_midCoder[posState]!!.Encode(rangeEncoder, sym)
					} else {
						rangeEncoder.Encode(_choice, 1, 1)
						_highCoder.Encode(rangeEncoder, sym - LzmaBase.kNumMidLenSymbols)
					}
				}
			}

			fun SetPrices(posState: Int, numSymbols: Int, prices: IntArray, st: Int) {
				val a0 = RangeEncoder.GetPrice0(_choice[0].toInt())
				val a1 = RangeEncoder.GetPrice1(_choice[0].toInt())
				val b0 = a1 + RangeEncoder.GetPrice0(_choice[1].toInt())
				val b1 = a1 + RangeEncoder.GetPrice1(_choice[1].toInt())
				var i = 0
				while (i < LzmaBase.kNumLowLenSymbols) {
					if (i >= numSymbols)
						return
					prices[st + i] = a0 + _lowCoder[posState]!!.GetPrice(i)
					i++
				}
				while (i < LzmaBase.kNumLowLenSymbols + LzmaBase.kNumMidLenSymbols) {
					if (i >= numSymbols)
						return
					prices[st + i] = b0 + _midCoder[posState]!!.GetPrice(i - LzmaBase.kNumLowLenSymbols)
					i++
				}
				while (i < numSymbols) {
					prices[st + i] = b1 +
							_highCoder.GetPrice(i - LzmaBase.kNumLowLenSymbols - LzmaBase.kNumMidLenSymbols)
					i++
				}
			}
		}

		internal inner class LenPriceTableEncoder : LenEncoder() {
			var _prices = IntArray(LzmaBase.kNumLenSymbols shl LzmaBase.kNumPosStatesBitsEncodingMax)
			var _tableSize: Int = 0
			var _counters = IntArray(LzmaBase.kNumPosStatesEncodingMax)

			fun SetTableSize(tableSize: Int) {
				_tableSize = tableSize
			}

			fun GetPrice(symbol: Int, posState: Int): Int {
				return _prices[posState * LzmaBase.kNumLenSymbols + symbol]
			}

			fun UpdateTable(posState: Int) {
				SetPrices(posState, _tableSize, _prices, posState * LzmaBase.kNumLenSymbols)
				_counters[posState] = _tableSize
			}

			fun UpdateTables(numPosStates: Int) {
				for (posState in 0 until numPosStates) {
					UpdateTable(posState)
				}
			}

			override fun Encode(
				rangeEncoder: RangeEncoder,
				symbol: Int,
				posState: Int
			) {
				super.Encode(rangeEncoder, symbol, posState)
				if (--_counters[posState] == 0)
					UpdateTable(posState)
			}
		}

		internal inner class Optimal {
			var State: Int = 0

			var Prev1IsChar: Boolean = false
			var Prev2: Boolean = false

			var PosPrev2: Int = 0
			var BackPrev2: Int = 0

			var Price: Int = 0
			var PosPrev: Int = 0
			var BackPrev: Int = 0

			var Backs0: Int = 0
			var Backs1: Int = 0
			var Backs2: Int = 0
			var Backs3: Int = 0

			fun MakeAsChar() {
				BackPrev = -1
				Prev1IsChar = false
			}

			fun MakeAsShortRep() {
				BackPrev = 0
				Prev1IsChar = false
			}

			fun IsShortRep(): Boolean = BackPrev == 0
		}

		internal fun Create() {
			if (_matchFinder == null) {
				val bt = LzBinTree()
				var numHashBytes = 4
				if (_matchFinderType == EMatchFinderTypeBT2)
					numHashBytes = 2
				bt.SetType(numHashBytes)
				_matchFinder = bt
			}
			_literalEncoder.Create(_numLiteralPosStateBits, _numLiteralContextBits)

			if (_dictionarySize == _dictionarySizePrev && _numFastBytesPrev == _numFastBytes)
				return
			_matchFinder!!.Create(
				_dictionarySize,
				kNumOpts, _numFastBytes, LzmaBase.kMatchMaxLen + 1
			)
			_dictionarySizePrev = _dictionarySize
			_numFastBytesPrev = _numFastBytes
		}

		internal fun SetWriteEndMarkerMode(writeEndMarker: Boolean) {
			_writeEndMark = writeEndMarker
		}

		internal fun Init() {
			BaseInit()
			_rangeEncoder.Init()

			RangeEncoder.InitBitModels(_isMatch)
			RangeEncoder.InitBitModels(_isRep0Long)
			RangeEncoder.InitBitModels(_isRep)
			RangeEncoder.InitBitModels(_isRepG0)
			RangeEncoder.InitBitModels(_isRepG1)
			RangeEncoder.InitBitModels(_isRepG2)
			RangeEncoder.InitBitModels(_posEncoders)

			_literalEncoder.Init()
			for (i in 0 until LzmaBase.kNumLenToPosStates) {
				_posSlotEncoder[i].Init()
			}

			_lenEncoder.Init(1 shl _posStateBits)
			_repMatchLenEncoder.Init(1 shl _posStateBits)

			_posAlignEncoder.Init()

			_longestMatchWasFound = false
			_optimumEndIndex = 0
			_optimumCurrentIndex = 0
			_additionalOffset = 0
		}

		internal fun ReadMatchDistances(): Int {
			var lenRes = 0
			_numDistancePairs = _matchFinder!!.GetMatches(_matchDistances)
			if (_numDistancePairs > 0) {
				lenRes = _matchDistances[_numDistancePairs - 2]
				if (lenRes == _numFastBytes)
					lenRes += _matchFinder!!.GetMatchLen(
						lenRes - 1, _matchDistances[_numDistancePairs - 1],
						LzmaBase.kMatchMaxLen - lenRes
					)
			}
			_additionalOffset++
			return lenRes
		}

		internal fun MovePos(num: Int) {
			if (num > 0) {
				_matchFinder!!.Skip(num)
				_additionalOffset += num
			}
		}

		internal fun GetRepLen1Price(state: Int, posState: Int): Int =
			RangeEncoder.GetPrice0(_isRepG0[state].toInt()) + RangeEncoder.GetPrice0(
				_isRep0Long[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
			)

		internal fun GetPureRepPrice(repIndex: Int, state: Int, posState: Int): Int {
			var price: Int
			if (repIndex == 0) {
				price = RangeEncoder.GetPrice0(_isRepG0[state].toInt())
				price += RangeEncoder.GetPrice1(
					_isRep0Long[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
				)
			} else {
				price = RangeEncoder.GetPrice1(_isRepG0[state].toInt())
				if (repIndex == 1) {
					price += RangeEncoder.GetPrice0(_isRepG1[state].toInt())
				} else {
					price += RangeEncoder.GetPrice1(_isRepG1[state].toInt())
					price += RangeEncoder.GetPrice(_isRepG2[state].toInt(), repIndex - 2)
				}
			}
			return price
		}

		internal fun GetRepPrice(repIndex: Int, len: Int, state: Int, posState: Int): Int {
			val price = _repMatchLenEncoder.GetPrice(len - LzmaBase.kMatchMinLen, posState)
			return price + GetPureRepPrice(repIndex, state, posState)
		}

		internal fun GetPosLenPrice(pos: Int, len: Int, posState: Int): Int {
			val price: Int
			val lenToPosState = LzmaBase.GetLenToPosState(len)
			if (pos < LzmaBase.kNumFullDistances)
				price = _distancesPrices[lenToPosState * LzmaBase.kNumFullDistances + pos]
			else
				price = _posSlotPrices[(lenToPosState shl LzmaBase.kNumPosSlotBits) + GetPosSlot2(
					pos
				)] +
						_alignPrices[pos and LzmaBase.kAlignMask]
			return price + _lenEncoder.GetPrice(len - LzmaBase.kMatchMinLen, posState)
		}

		internal fun Backward(cur: Int): Int {
			var cc = cur
			_optimumEndIndex = cc
			var posMem = _optimum[cc].PosPrev
			var backMem = _optimum[cc].BackPrev
			do {
				if (_optimum[cc].Prev1IsChar) {
					_optimum[posMem].MakeAsChar()
					_optimum[posMem].PosPrev = posMem - 1
					if (_optimum[cc].Prev2) {
						_optimum[posMem - 1].Prev1IsChar = false
						_optimum[posMem - 1].PosPrev = _optimum[cc].PosPrev2
						_optimum[posMem - 1].BackPrev = _optimum[cc].BackPrev2
					}
				}
				val posPrev = posMem
				val backCur = backMem

				backMem = _optimum[posPrev].BackPrev
				posMem = _optimum[posPrev].PosPrev

				_optimum[posPrev].BackPrev = backCur
				_optimum[posPrev].PosPrev = cc
				cc = posPrev
			} while (cc > 0)
			backRes = _optimum[0].BackPrev
			_optimumCurrentIndex = _optimum[0].PosPrev
			return _optimumCurrentIndex
		}

		internal fun GetOptimum(position: Int): Int {
			var ppos = position
			if (_optimumEndIndex != _optimumCurrentIndex) {
				val lenRes = _optimum[_optimumCurrentIndex].PosPrev - _optimumCurrentIndex
				backRes = _optimum[_optimumCurrentIndex].BackPrev
				_optimumCurrentIndex = _optimum[_optimumCurrentIndex].PosPrev
				return lenRes
			}
			_optimumEndIndex = 0
			_optimumCurrentIndex = _optimumEndIndex

			val lenMain: Int
			var numDistancePairs: Int
			if (!_longestMatchWasFound) {
				lenMain = ReadMatchDistances()
			} else {
				lenMain = _longestMatchLength
				_longestMatchWasFound = false
			}
			numDistancePairs = _numDistancePairs

			var numAvailableBytes = _matchFinder!!.GetNumAvailableBytes() + 1
			if (numAvailableBytes < 2) {
				backRes = -1
				return 1
			}
			if (numAvailableBytes > LzmaBase.kMatchMaxLen) {
				numAvailableBytes = LzmaBase.kMatchMaxLen
			}

			var repMaxIndex = 0
			var i: Int = 0
			while (i < LzmaBase.kNumRepDistances) {
				reps[i] = _repDistances[i]
				repLens[i] = _matchFinder!!.GetMatchLen(
					0 - 1, reps[i],
					LzmaBase.kMatchMaxLen
				)
				if (repLens[i] > repLens[repMaxIndex])
					repMaxIndex = i
				i++
			}
			if (repLens[repMaxIndex] >= _numFastBytes) {
				backRes = repMaxIndex
				val lenRes = repLens[repMaxIndex]
				MovePos(lenRes - 1)
				return lenRes
			}

			if (lenMain >= _numFastBytes) {
				backRes = _matchDistances[numDistancePairs - 1] +
						LzmaBase.kNumRepDistances
				MovePos(lenMain - 1)
				return lenMain
			}

			var currentByte = _matchFinder!!.GetIndexByte(0 - 1)
			var matchByte = _matchFinder!!.GetIndexByte(0 - _repDistances[0] - 1 - 1)

			if (lenMain < 2 && currentByte != matchByte && repLens[repMaxIndex] < 2) {
				backRes = -1
				return 1
			}

			_optimum[0].State = _state

			var posState = ppos and _posStateMask

			_optimum[1].Price = RangeEncoder.GetPrice0(
				_isMatch[(_state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
			) +
					_literalEncoder.GetSubCoder(ppos, _previousByte).GetPrice(
						!LzmaBase.StateIsCharState(_state),
						matchByte,
						currentByte
					)
			_optimum[1].MakeAsChar()

			var matchPrice =
				RangeEncoder.GetPrice1(_isMatch[(_state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt())
			var repMatchPrice =
				matchPrice + RangeEncoder.GetPrice1(
					_isRep[_state].toInt()
				)

			if (matchByte == currentByte) {
				val shortRepPrice = repMatchPrice + GetRepLen1Price(_state, posState)
				if (shortRepPrice < _optimum[1].Price) {
					_optimum[1].Price = shortRepPrice
					_optimum[1].MakeAsShortRep()
				}
			}

			var lenEnd = if (lenMain >= repLens[repMaxIndex]) lenMain else repLens[repMaxIndex]

			if (lenEnd < 2) {
				backRes = _optimum[1].BackPrev
				return 1
			}

			_optimum[1].PosPrev = 0

			_optimum[0].Backs0 = reps[0]
			_optimum[0].Backs1 = reps[1]
			_optimum[0].Backs2 = reps[2]
			_optimum[0].Backs3 = reps[3]

			var len = lenEnd
			do
				_optimum[len--].Price =
						kIfinityPrice
			while (len >= 2)

			i = 0
			while (i < LzmaBase.kNumRepDistances) {
				var repLen = repLens[i]
				if (repLen < 2) {
					i++
					continue
				}
				val price = repMatchPrice + GetPureRepPrice(i, _state, posState)
				do {
					val curAndLenPrice = price + _repMatchLenEncoder.GetPrice(repLen - 2, posState)
					val optimum = _optimum[repLen]
					if (curAndLenPrice < optimum.Price) {
						optimum.Price = curAndLenPrice
						optimum.PosPrev = 0
						optimum.BackPrev = i
						optimum.Prev1IsChar = false
					}
				} while (--repLen >= 2)
				i++
			}

			var normalMatchPrice =
				matchPrice + RangeEncoder.GetPrice0(
					_isRep[_state].toInt()
				)

			len = if (repLens[0] >= 2) repLens[0] + 1 else 2
			if (len <= lenMain) {
				var offs = 0
				while (len > _matchDistances[offs])
					offs += 2
				while (true) {
					val distance = _matchDistances[offs + 1]
					val curAndLenPrice = normalMatchPrice + GetPosLenPrice(distance, len, posState)
					val optimum = _optimum[len]
					if (curAndLenPrice < optimum.Price) {
						optimum.Price = curAndLenPrice
						optimum.PosPrev = 0
						optimum.BackPrev = distance +
								LzmaBase.kNumRepDistances
						optimum.Prev1IsChar = false
					}
					if (len == _matchDistances[offs]) {
						offs += 2
						if (offs == numDistancePairs)
							break
					}
					len++
				}
			}

			var cur = 0

			while (true) {
				cur++
				if (cur == lenEnd)
					return Backward(cur)
				var newLen = ReadMatchDistances()
				numDistancePairs = _numDistancePairs
				if (newLen >= _numFastBytes) {

					_longestMatchLength = newLen
					_longestMatchWasFound = true
					return Backward(cur)
				}
				ppos++
				var posPrev = _optimum[cur].PosPrev
				var state: Int
				if (_optimum[cur].Prev1IsChar) {
					posPrev--
					if (_optimum[cur].Prev2) {
						state = _optimum[_optimum[cur].PosPrev2].State
						if (_optimum[cur].BackPrev2 < LzmaBase.kNumRepDistances)
							state = LzmaBase.StateUpdateRep(state)
						else
							state = LzmaBase.StateUpdateMatch(state)
					} else
						state = _optimum[posPrev].State
					state = LzmaBase.StateUpdateChar(state)
				} else
					state = _optimum[posPrev].State
				if (posPrev == cur - 1) {
					if (_optimum[cur].IsShortRep())
						state = LzmaBase.StateUpdateShortRep(state)
					else
						state = LzmaBase.StateUpdateChar(state)
				} else {
					val pos: Int
					if (_optimum[cur].Prev1IsChar && _optimum[cur].Prev2) {
						posPrev = _optimum[cur].PosPrev2
						pos = _optimum[cur].BackPrev2
						state = LzmaBase.StateUpdateRep(state)
					} else {
						pos = _optimum[cur].BackPrev
						if (pos < LzmaBase.kNumRepDistances)
							state = LzmaBase.StateUpdateRep(state)
						else
							state = LzmaBase.StateUpdateMatch(state)
					}
					val opt = _optimum[posPrev]
					if (pos < LzmaBase.kNumRepDistances) {
						if (pos == 0) {
							reps[0] = opt.Backs0
							reps[1] = opt.Backs1
							reps[2] = opt.Backs2
							reps[3] = opt.Backs3
						} else if (pos == 1) {
							reps[0] = opt.Backs1
							reps[1] = opt.Backs0
							reps[2] = opt.Backs2
							reps[3] = opt.Backs3
						} else if (pos == 2) {
							reps[0] = opt.Backs2
							reps[1] = opt.Backs0
							reps[2] = opt.Backs1
							reps[3] = opt.Backs3
						} else {
							reps[0] = opt.Backs3
							reps[1] = opt.Backs0
							reps[2] = opt.Backs1
							reps[3] = opt.Backs2
						}
					} else {
						reps[0] = pos - LzmaBase.kNumRepDistances
						reps[1] = opt.Backs0
						reps[2] = opt.Backs1
						reps[3] = opt.Backs2
					}
				}
				_optimum[cur].State = state
				_optimum[cur].Backs0 = reps[0]
				_optimum[cur].Backs1 = reps[1]
				_optimum[cur].Backs2 = reps[2]
				_optimum[cur].Backs3 = reps[3]
				val curPrice = _optimum[cur].Price

				currentByte = _matchFinder!!.GetIndexByte(0 - 1)
				matchByte = _matchFinder!!.GetIndexByte(0 - reps[0] - 1 - 1)

				posState = ppos and _posStateMask

				val curAnd1Price = curPrice +
						RangeEncoder.GetPrice0(
							_isMatch[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
						) +
						_literalEncoder.GetSubCoder(
							ppos,
							_matchFinder!!.GetIndexByte(0 - 2)
						).GetPrice(!LzmaBase.StateIsCharState(state), matchByte, currentByte)

				val nextOptimum = _optimum[cur + 1]

				var nextIsChar = false
				if (curAnd1Price < nextOptimum.Price) {
					nextOptimum.Price = curAnd1Price
					nextOptimum.PosPrev = cur
					nextOptimum.MakeAsChar()
					nextIsChar = true
				}

				matchPrice = curPrice +
						RangeEncoder.GetPrice1(
							_isMatch[(state shl LzmaBase.kNumPosStatesBitsMax) + posState].toInt()
						)
				repMatchPrice = matchPrice +
						RangeEncoder.GetPrice1(
							_isRep[state].toInt()
						)

				if (matchByte == currentByte && !(nextOptimum.PosPrev < cur && nextOptimum.BackPrev == 0)) {
					val shortRepPrice = repMatchPrice + GetRepLen1Price(state, posState)
					if (shortRepPrice <= nextOptimum.Price) {
						nextOptimum.Price = shortRepPrice
						nextOptimum.PosPrev = cur
						nextOptimum.MakeAsShortRep()
						nextIsChar = true
					}
				}

				var numAvailableBytesFull = _matchFinder!!.GetNumAvailableBytes() + 1
				numAvailableBytesFull = min(kNumOpts - 1 - cur, numAvailableBytesFull)
				numAvailableBytes = numAvailableBytesFull

				if (numAvailableBytes < 2)
					continue
				if (numAvailableBytes > _numFastBytes)
					numAvailableBytes = _numFastBytes
				if (!nextIsChar && matchByte != currentByte) {
					// try Literal + rep0
					val t = min(numAvailableBytesFull - 1, _numFastBytes)
					val lenTest2 = _matchFinder!!.GetMatchLen(0, reps[0], t)
					if (lenTest2 >= 2) {
						val state2 = LzmaBase.StateUpdateChar(state)

						val posStateNext = ppos + 1 and _posStateMask
						val nextRepMatchPrice = curAnd1Price +
								RangeEncoder.GetPrice1(
									_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
								) +
								RangeEncoder.GetPrice1(
									_isRep[state2].toInt()
								)
						run {
							val offset = cur + 1 + lenTest2
							while (lenEnd < offset)
								_optimum[++lenEnd].Price =
										kIfinityPrice
							val curAndLenPrice = nextRepMatchPrice + GetRepPrice(
								0, lenTest2, state2, posStateNext
							)
							val optimum = _optimum[offset]
							if (curAndLenPrice < optimum.Price) {
								optimum.Price = curAndLenPrice
								optimum.PosPrev = cur + 1
								optimum.BackPrev = 0
								optimum.Prev1IsChar = true
								optimum.Prev2 = false
							}
						}
					}
				}

				var startLen = 2 // speed optimization

				for (repIndex in 0 until LzmaBase.kNumRepDistances) {
					var lenTest = _matchFinder!!.GetMatchLen(0 - 1, reps[repIndex], numAvailableBytes)
					if (lenTest < 2)
						continue
					val lenTestTemp = lenTest
					do {
						while (lenEnd < cur + lenTest)
							_optimum[++lenEnd].Price =
									kIfinityPrice
						val curAndLenPrice = repMatchPrice + GetRepPrice(repIndex, lenTest, state, posState)
						val optimum = _optimum[cur + lenTest]
						if (curAndLenPrice < optimum.Price) {
							optimum.Price = curAndLenPrice
							optimum.PosPrev = cur
							optimum.BackPrev = repIndex
							optimum.Prev1IsChar = false
						}
					} while (--lenTest >= 2)
					lenTest = lenTestTemp

					if (repIndex == 0)
						startLen = lenTest + 1

					// if (_maxMode)
					if (lenTest < numAvailableBytesFull) {
						val t = min(numAvailableBytesFull - 1 - lenTest, _numFastBytes)
						val lenTest2 = _matchFinder!!.GetMatchLen(lenTest, reps[repIndex], t)
						if (lenTest2 >= 2) {
							var state2 =
								LzmaBase.StateUpdateRep(state)

							var posStateNext = ppos + lenTest and _posStateMask
							val curAndLenCharPrice = repMatchPrice + GetRepPrice(repIndex, lenTest, state, posState) +
									RangeEncoder.GetPrice0(
										_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
									) +
									_literalEncoder.GetSubCoder(
										ppos + lenTest,
										_matchFinder!!.GetIndexByte(lenTest - 1 - 1)
									).GetPrice(
										true,
										_matchFinder!!.GetIndexByte(lenTest - 1 - (reps[repIndex] + 1)),
										_matchFinder!!.GetIndexByte(lenTest - 1)
									)
							state2 = LzmaBase.StateUpdateChar(state2)
							posStateNext = ppos + lenTest + 1 and _posStateMask
							val nextMatchPrice =
								curAndLenCharPrice + RangeEncoder.GetPrice1(
									_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
								)
							val nextRepMatchPrice =
								nextMatchPrice + RangeEncoder.GetPrice1(
									_isRep[state2].toInt()
								)

							// for(; lenTest2 >= 2; lenTest2--)
							run {
								val offset = lenTest + 1 + lenTest2
								while (lenEnd < cur + offset)
									_optimum[++lenEnd].Price =
											kIfinityPrice
								val curAndLenPrice = nextRepMatchPrice + GetRepPrice(0, lenTest2, state2, posStateNext)
								val optimum = _optimum[cur + offset]
								if (curAndLenPrice < optimum.Price) {
									optimum.Price = curAndLenPrice
									optimum.PosPrev = cur + lenTest + 1
									optimum.BackPrev = 0
									optimum.Prev1IsChar = true
									optimum.Prev2 = true
									optimum.PosPrev2 = cur
									optimum.BackPrev2 = repIndex
								}
							}
						}
					}
				}

				if (newLen > numAvailableBytes) {
					newLen = numAvailableBytes
					numDistancePairs = 0
					while (newLen > _matchDistances[numDistancePairs]) {
						numDistancePairs += 2
					}
					_matchDistances[numDistancePairs] = newLen
					numDistancePairs += 2
				}
				if (newLen >= startLen) {
					normalMatchPrice = matchPrice +
							RangeEncoder.GetPrice0(
								_isRep[state].toInt()
							)
					while (lenEnd < cur + newLen)
						_optimum[++lenEnd].Price =
								kIfinityPrice

					var offs = 0
					while (startLen > _matchDistances[offs])
						offs += 2

					var lenTest = startLen
					while (true) {
						val curBack = _matchDistances[offs + 1]
						var curAndLenPrice = normalMatchPrice + GetPosLenPrice(curBack, lenTest, posState)
						var optimum = _optimum[cur + lenTest]
						if (curAndLenPrice < optimum.Price) {
							optimum.Price = curAndLenPrice
							optimum.PosPrev = cur
							optimum.BackPrev = curBack +
									LzmaBase.kNumRepDistances
							optimum.Prev1IsChar = false
						}

						if (lenTest == _matchDistances[offs]) {
							if (lenTest < numAvailableBytesFull) {
								val t = min(numAvailableBytesFull - 1 - lenTest, _numFastBytes)
								val lenTest2 = _matchFinder!!.GetMatchLen(lenTest, curBack, t)
								if (lenTest2 >= 2) {
									var state2 =
										LzmaBase.StateUpdateMatch(
											state
										)

									var posStateNext = ppos + lenTest and _posStateMask
									val curAndLenCharPrice = curAndLenPrice +
											RangeEncoder.GetPrice0(
												_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
											) +
											_literalEncoder.GetSubCoder(
												ppos + lenTest,
												_matchFinder!!.GetIndexByte(lenTest - 1 - 1)
											).GetPrice(
												true,
												_matchFinder!!.GetIndexByte(lenTest - (curBack + 1) - 1),
												_matchFinder!!.GetIndexByte(lenTest - 1)
											)
									state2 =
											LzmaBase.StateUpdateChar(
												state2
											)
									posStateNext = ppos + lenTest + 1 and _posStateMask
									val nextMatchPrice =
										curAndLenCharPrice + RangeEncoder.GetPrice1(
											_isMatch[(state2 shl LzmaBase.kNumPosStatesBitsMax) + posStateNext].toInt()
										)
									val nextRepMatchPrice =
										nextMatchPrice + RangeEncoder.GetPrice1(
											_isRep[state2].toInt()
										)

									val offset = lenTest + 1 + lenTest2
									while (lenEnd < cur + offset)
										_optimum[++lenEnd].Price =
												kIfinityPrice
									curAndLenPrice = nextRepMatchPrice + GetRepPrice(0, lenTest2, state2, posStateNext)
									optimum = _optimum[cur + offset]
									if (curAndLenPrice < optimum.Price) {
										optimum.Price = curAndLenPrice
										optimum.PosPrev = cur + lenTest + 1
										optimum.BackPrev = 0
										optimum.Prev1IsChar = true
										optimum.Prev2 = true
										optimum.PosPrev2 = cur
										optimum.BackPrev2 = curBack +
												LzmaBase.kNumRepDistances
									}
								}
							}
							offs += 2
							if (offs == numDistancePairs)
								break
						}
						lenTest++
					}
				}
			}
		}

		internal fun ChangePair(smallDist: Int, bigDist: Int): Boolean {
			val kDif = 7
			return smallDist < 1 shl 32 - kDif && bigDist >= smallDist shl kDif
		}

		internal fun WriteEndMarker(posState: Int) {
			if (!_writeEndMark)
				return

			_rangeEncoder.Encode(_isMatch, (_state shl LzmaBase.kNumPosStatesBitsMax) + posState, 1)
			_rangeEncoder.Encode(_isRep, _state, 0)
			_state = LzmaBase.StateUpdateMatch(_state)
			val len = LzmaBase.kMatchMinLen
			_lenEncoder.Encode(_rangeEncoder, len - LzmaBase.kMatchMinLen, posState)
			val posSlot = (1 shl LzmaBase.kNumPosSlotBits) - 1
			val lenToPosState = LzmaBase.GetLenToPosState(len)
			_posSlotEncoder[lenToPosState].Encode(_rangeEncoder, posSlot)
			val footerBits = 30
			val posReduced = (1 shl footerBits) - 1
			_rangeEncoder.EncodeDirectBits(posReduced shr LzmaBase.kNumAlignBits, footerBits - LzmaBase.kNumAlignBits)
			_posAlignEncoder.ReverseEncode(_rangeEncoder, posReduced and LzmaBase.kAlignMask)
		}

		internal fun Flush(nowPos: Int) {
			ReleaseMFStream()
			WriteEndMarker(nowPos and _posStateMask)
			_rangeEncoder.FlushData()
			_rangeEncoder.FlushStream()
		}

		fun CodeOneBlock(inSize: LongArray, outSize: LongArray, finished: BooleanArray) {
			inSize[0] = 0
			outSize[0] = 0
			finished[0] = true

			if (_inStream != null) {
				_matchFinder!!.SetStream(_inStream!!)
				_matchFinder!!.Init()
				_needReleaseMFStream = true
				_inStream = null
			}

			if (_finished)
				return
			_finished = true


			val progressPosValuePrev = nowPos64
			if (nowPos64 == 0L) {
				if (_matchFinder!!.GetNumAvailableBytes() == 0) {
					Flush(nowPos64.toInt())
					return
				}

				ReadMatchDistances()
				val posState = nowPos64.toInt() and _posStateMask
				_rangeEncoder.Encode(_isMatch, (_state shl LzmaBase.kNumPosStatesBitsMax) + posState, 0)
				_state = LzmaBase.StateUpdateChar(_state)
				val curByte = _matchFinder!!.GetIndexByte(0 - _additionalOffset)
				_literalEncoder.GetSubCoder(nowPos64.toInt(), _previousByte).Encode(_rangeEncoder, curByte)
				_previousByte = curByte
				_additionalOffset--
				nowPos64++
			}
			if (_matchFinder!!.GetNumAvailableBytes() == 0) {
				Flush(nowPos64.toInt())
				return
			}
			while (true) {

				val len = GetOptimum(nowPos64.toInt())
				var pos = backRes
				val posState = nowPos64.toInt() and _posStateMask
				val complexState = (_state shl LzmaBase.kNumPosStatesBitsMax) + posState
				if (len == 1 && pos == -1) {
					_rangeEncoder.Encode(_isMatch, complexState, 0)
					val curByte = _matchFinder!!.GetIndexByte(0 - _additionalOffset)
					val subCoder = _literalEncoder.GetSubCoder(nowPos64.toInt(), _previousByte)
					if (!LzmaBase.StateIsCharState(_state)) {
						val matchByte = _matchFinder!!.GetIndexByte(0 - _repDistances[0] - 1 - _additionalOffset)
						subCoder.EncodeMatched(_rangeEncoder, matchByte, curByte)
					} else
						subCoder.Encode(_rangeEncoder, curByte)
					_previousByte = curByte
					_state = LzmaBase.StateUpdateChar(_state)
				} else {
					_rangeEncoder.Encode(_isMatch, complexState, 1)
					if (pos < LzmaBase.kNumRepDistances) {
						_rangeEncoder.Encode(_isRep, _state, 1)
						if (pos == 0) {
							_rangeEncoder.Encode(_isRepG0, _state, 0)
							if (len == 1)
								_rangeEncoder.Encode(_isRep0Long, complexState, 0)
							else
								_rangeEncoder.Encode(_isRep0Long, complexState, 1)
						} else {
							_rangeEncoder.Encode(_isRepG0, _state, 1)
							if (pos == 1)
								_rangeEncoder.Encode(_isRepG1, _state, 0)
							else {
								_rangeEncoder.Encode(_isRepG1, _state, 1)
								_rangeEncoder.Encode(_isRepG2, _state, pos - 2)
							}
						}
						if (len == 1)
							_state = LzmaBase.StateUpdateShortRep(
								_state
							)
						else {
							_repMatchLenEncoder.Encode(_rangeEncoder, len - LzmaBase.kMatchMinLen, posState)
							_state = LzmaBase.StateUpdateRep(_state)
						}
						val distance = _repDistances[pos]
						if (pos != 0) {
							for (i in pos downTo 1)
								_repDistances[i] = _repDistances[i - 1]
							_repDistances[0] = distance
						}
					} else {
						_rangeEncoder.Encode(_isRep, _state, 0)
						_state = LzmaBase.StateUpdateMatch(_state)
						_lenEncoder.Encode(_rangeEncoder, len - LzmaBase.kMatchMinLen, posState)
						pos -= LzmaBase.kNumRepDistances
						val posSlot =
							GetPosSlot(pos)
						val lenToPosState =
							LzmaBase.GetLenToPosState(len)
						_posSlotEncoder[lenToPosState].Encode(_rangeEncoder, posSlot)

						if (posSlot >= LzmaBase.kStartPosModelIndex) {
							val footerBits = (posSlot shr 1) - 1
							val baseVal = 2 or (posSlot and 1) shl footerBits
							val posReduced = pos - baseVal

							if (posSlot < LzmaBase.kEndPosModelIndex)
								BitTreeEncoder.ReverseEncode(
									_posEncoders,
									baseVal - posSlot - 1, _rangeEncoder, footerBits, posReduced
								)
							else {
								_rangeEncoder.EncodeDirectBits(
									posReduced shr LzmaBase.kNumAlignBits,
									footerBits - LzmaBase.kNumAlignBits
								)
								_posAlignEncoder.ReverseEncode(_rangeEncoder, posReduced and LzmaBase.kAlignMask)
								_alignPriceCount++
							}
						}
						val distance = pos
						for (i in LzmaBase.kNumRepDistances - 1 downTo 1)
							_repDistances[i] = _repDistances[i - 1]
						_repDistances[0] = distance
						_matchPriceCount++
					}
					_previousByte = _matchFinder!!.GetIndexByte(len - 1 - _additionalOffset)
				}
				_additionalOffset -= len
				nowPos64 += len.toLong()
				if (_additionalOffset == 0) {
					// if (!_fastMode)
					if (_matchPriceCount >= 1 shl 7)
						FillDistancesPrices()
					if (_alignPriceCount >= LzmaBase.kAlignTableSize)
						FillAlignPrices()
					inSize[0] = nowPos64
					outSize[0] = _rangeEncoder.GetProcessedSizeAdd()
					if (_matchFinder!!.GetNumAvailableBytes() == 0) {
						Flush(nowPos64.toInt())
						return
					}

					if (nowPos64 - progressPosValuePrev >= 1 shl 12) {
						_finished = false
						finished[0] = false
						return
					}
				}
			}
		}

		internal fun ReleaseMFStream() {
			if (_matchFinder != null && _needReleaseMFStream) {
				_matchFinder!!.ReleaseStream()
				_needReleaseMFStream = false
			}
		}

		internal fun SetOutStream(outStream: SyncOutputStream) {
			_rangeEncoder.SetStream(outStream)
		}

		internal fun ReleaseOutStream() {
			_rangeEncoder.ReleaseStream()
		}

		internal fun ReleaseStreams() {
			ReleaseMFStream()
			ReleaseOutStream()
		}

		internal fun SetStreams(
			inStream: SyncInputStream, outStream: SyncOutputStream,
			inSize: Long, outSize: Long
		) {
			_inStream = inStream
			_finished = false
			Create()
			SetOutStream(outStream)
			Init()

			// if (!_fastMode)
			run {
				FillDistancesPrices()
				FillAlignPrices()
			}

			_lenEncoder.SetTableSize(_numFastBytes + 1 - LzmaBase.kMatchMinLen)
			_lenEncoder.UpdateTables(1 shl _posStateBits)
			_repMatchLenEncoder.SetTableSize(_numFastBytes + 1 - LzmaBase.kMatchMinLen)
			_repMatchLenEncoder.UpdateTables(1 shl _posStateBits)

			nowPos64 = 0
		}

		fun Code(
			inStream: SyncInputStream, outStream: SyncOutputStream,
			inSize: Long, outSize: Long, progress: ICodeProgress?
		) {
			_needReleaseMFStream = false
			try {
				SetStreams(inStream, outStream, inSize, outSize)
				while (true) {


					CodeOneBlock(processedInSize, processedOutSize, finished)
					if (finished[0])
						return
					if (progress != null) {
						progress.SetProgress(processedInSize[0], processedOutSize[0])
					}
				}
			} finally {
				ReleaseStreams()
			}
		}

		fun WriteCoderProperties(outStream: SyncOutputStream) {
			properties[0] = ((_posStateBits * 5 + _numLiteralPosStateBits) * 9 + _numLiteralContextBits).toByte()
			for (i in 0..3)
				properties[1 + i] = (_dictionarySize shr 8 * i).toByte()
			outStream.write(
				properties, 0,
				kPropSize
			)
		}

		internal fun FillDistancesPrices() {
			for (i in LzmaBase.kStartPosModelIndex until LzmaBase.kNumFullDistances) {
				val posSlot = GetPosSlot(i)
				val footerBits = (posSlot shr 1) - 1
				val baseVal = 2 or (posSlot and 1) shl footerBits
				tempPrices[i] =
						BitTreeEncoder.ReverseGetPrice(
							_posEncoders,
							baseVal - posSlot - 1, footerBits, i - baseVal
						)
			}

			for (lenToPosState in 0 until LzmaBase.kNumLenToPosStates) {
				var posSlot: Int
				val encoder = _posSlotEncoder[lenToPosState]

				val st = lenToPosState shl LzmaBase.kNumPosSlotBits
				posSlot = 0
				while (posSlot < _distTableSize) {
					_posSlotPrices[st + posSlot] = encoder.GetPrice(posSlot)
					posSlot++
				}
				posSlot = LzmaBase.kEndPosModelIndex
				while (posSlot < _distTableSize) {
					_posSlotPrices[st + posSlot] += (posSlot shr 1) - 1 - LzmaBase.kNumAlignBits shl RangeEncoder.kNumBitPriceShiftBits
					posSlot++
				}

				val st2 = lenToPosState * LzmaBase.kNumFullDistances
				var i: Int
				i = 0
				while (i < LzmaBase.kStartPosModelIndex) {
					_distancesPrices[st2 + i] = _posSlotPrices[st + i]
					i++
				}
				while (i < LzmaBase.kNumFullDistances) {
					_distancesPrices[st2 + i] = _posSlotPrices[st + GetPosSlot(
						i
					)] + tempPrices[i]
					i++
				}
			}
			_matchPriceCount = 0
		}

		internal fun FillAlignPrices() {
			for (i in 0 until LzmaBase.kAlignTableSize)
				_alignPrices[i] = _posAlignEncoder.ReverseGetPrice(i)
			_alignPriceCount = 0
		}


		fun SetAlgorithm(algorithm: Int): Boolean {
			/*
        _fastMode = (algorithm == 0);
        _maxMode = (algorithm >= 2);
        */
			return true
		}

		fun SetDictionarySize(dictionarySize: Int): Boolean {
			val kDicLogSizeMaxCompress = 29
			val cond1 = dictionarySize < (1 shl LzmaBase.kDicLogSizeMin)
			val cond2 = dictionarySize > (1 shl kDicLogSizeMaxCompress)
			if (cond1 || cond2)
				return false
			_dictionarySize = dictionarySize
			var dicLogSize: Int
			dicLogSize = 0
			while (dictionarySize > 1 shl dicLogSize) {
				dicLogSize++
			}
			_distTableSize = dicLogSize * 2
			return true
		}

		fun SetNumFastBytes(numFastBytes: Int): Boolean {
			if (numFastBytes < 5 || numFastBytes > LzmaBase.kMatchMaxLen)
				return false
			_numFastBytes = numFastBytes
			return true
		}

		fun SetMatchFinder(matchFinderIndex: Int): Boolean {
			if (matchFinderIndex < 0 || matchFinderIndex > 2)
				return false
			val matchFinderIndexPrev = _matchFinderType
			_matchFinderType = matchFinderIndex
			if (_matchFinder != null && matchFinderIndexPrev != _matchFinderType) {
				_dictionarySizePrev = -1
				_matchFinder = null
			}
			return true
		}

		fun SetLcLpPb(lc: Int, lp: Int, pb: Int): Boolean {
			if (lp < 0 || lp > LzmaBase.kNumLitPosStatesBitsEncodingMax ||
				lc < 0 || lc > LzmaBase.kNumLitContextBitsMax ||
				pb < 0 || pb > LzmaBase.kNumPosStatesBitsEncodingMax
			)
				return false
			_numLiteralPosStateBits = lp
			_numLiteralContextBits = lc
			_posStateBits = pb
			_posStateMask = (1 shl _posStateBits) - 1
			return true
		}

		fun SetEndMarkerMode(endMarkerMode: Boolean) {
			_writeEndMark = endMarkerMode
		}

		companion object {
			val EMatchFinderTypeBT2 = 0
			val EMatchFinderTypeBT4 = 1


			internal val kIfinityPrice = 0xFFFFFFF

			internal var g_FastPos = ByteArray(1 shl 11)

			init {
				val kFastSlots = 22
				var c = 2
				g_FastPos[0] = 0
				g_FastPos[1] = 1
				for (slotFast in 2 until kFastSlots) {
					val k = 1 shl (slotFast shr 1) - 1
					var j = 0
					while (j < k) {
						g_FastPos[c] = slotFast.toByte()
						j++
						c++
					}
				}
			}

			internal fun GetPosSlot(pos: Int): Int {
				if (pos < 1 shl 11)
					return g_FastPos[pos].toInt()
				return if (pos < 1 shl 21) g_FastPos[pos shr 10] + 20 else g_FastPos[pos shr 20] + 40
			}

			internal fun GetPosSlot2(pos: Int): Int {
				if (pos < 1 shl 17)
					return g_FastPos[pos shr 6] + 12
				return if (pos < 1 shl 27) g_FastPos[pos shr 16] + 32 else g_FastPos[pos shr 26] + 52
			}

			internal const val kDefaultDictionaryLogSize = 22
			internal const val kNumFastBytesDefault = 0x20

			const val kNumLenSpecSymbols = LzmaBase.kNumLowLenSymbols + LzmaBase.kNumMidLenSymbols

			internal const val kNumOpts = 1 shl 12

			val kPropSize = 5
		}
	}

	class LzBinTree : LzInWindow() {
		internal var _cyclicBufferPos: Int = 0
		internal var _cyclicBufferSize = 0
		internal var _matchMaxLen: Int = 0

		internal lateinit var _son: IntArray
		internal lateinit var _hash: IntArray

		internal var _cutValue = 0xFF
		internal var _hashMask: Int = 0
		internal var _hashSizeSum = 0

		internal var HASH_ARRAY = true

		internal var kNumHashDirectBytes = 0
		internal var kMinMatchCheck = 4
		internal var kFixHashSize = kHash2Size + kHash3Size

		fun SetType(numHashBytes: Int) {
			HASH_ARRAY = numHashBytes > 2
			if (HASH_ARRAY) {
				kNumHashDirectBytes = 0
				kMinMatchCheck = 4
				kFixHashSize = kHash2Size +
						kHash3Size
			} else {
				kNumHashDirectBytes = 2
				kMinMatchCheck = 2 + 1
				kFixHashSize = 0
			}
		}


		override fun Init() {
			super.Init()
			for (i in 0 until _hashSizeSum)
				_hash[i] = kEmptyHashValue
			_cyclicBufferPos = 0
			ReduceOffsets(-1)
		}

		override fun MovePos() {
			if (++_cyclicBufferPos >= _cyclicBufferSize)
				_cyclicBufferPos = 0
			super.MovePos()
			if (_pos == kMaxValForNormalize)
				Normalize()
		}


		fun Create(
			historySize: Int, keepAddBufferBefore: Int,
			matchMaxLen: Int, keepAddBufferAfter: Int
		): Boolean {
			if (historySize > kMaxValForNormalize - 256)
				return false
			_cutValue = 16 + (matchMaxLen shr 1)

			val windowReservSize = (historySize + keepAddBufferBefore +
					matchMaxLen + keepAddBufferAfter) / 2 + 256

			super.Create(historySize + keepAddBufferBefore, matchMaxLen + keepAddBufferAfter, windowReservSize)

			_matchMaxLen = matchMaxLen

			val cyclicBufferSize = historySize + 1
			if (_cyclicBufferSize != cyclicBufferSize)
				_cyclicBufferSize = cyclicBufferSize
			_son = IntArray(_cyclicBufferSize * 2)

			var hs = kBT2HashSize

			if (HASH_ARRAY) {
				hs = historySize - 1
				hs = hs or (hs shr 1)
				hs = hs or (hs shr 2)
				hs = hs or (hs shr 4)
				hs = hs or (hs shr 8)
				hs = hs shr 1
				hs = hs or 0xFFFF
				if (hs > 1 shl 24)
					hs = hs shr 1
				_hashMask = hs
				hs++
				hs += kFixHashSize
			}
			if (hs != _hashSizeSum) {
				_hashSizeSum = hs
				_hash = IntArray(_hashSizeSum)
			}
			return true
		}

		fun GetMatches(distances: IntArray): Int {
			val lenLimit: Int
			if (_pos + _matchMaxLen <= _streamPos)
				lenLimit = _matchMaxLen
			else {
				lenLimit = _streamPos - _pos
				if (lenLimit < kMinMatchCheck) {
					MovePos()
					return 0
				}
			}

			var offset = 0
			val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
			val cur = _bufferOffset + _pos
			var maxLen =
				kStartMaxLen // to avoid items for len < hashSize;
			val hashValue: Int
			var hash2Value = 0
			var hash3Value = 0

			if (HASH_ARRAY) {
				var temp = CrcTable[_bufferBase!![cur] and 0xFF] xor (_bufferBase!![cur + 1] and 0xFF)
				hash2Value = temp and kHash2Size - 1
				temp = temp xor ((_bufferBase!![cur + 2] and 0xFF) shl 8)
				hash3Value = temp and kHash3Size - 1
				hashValue = temp xor (CrcTable[_bufferBase!![cur + 3] and 0xFF] shl 5) and _hashMask
			} else
				hashValue = _bufferBase!![cur] and 0xFF xor ((_bufferBase!![cur + 1] and 0xFF) shl 8)

			var curMatch = _hash[kFixHashSize + hashValue]
			if (HASH_ARRAY) {
				var curMatch2 = _hash[hash2Value]
				val curMatch3 = _hash[kHash3Offset + hash3Value]
				_hash[hash2Value] = _pos
				_hash[kHash3Offset + hash3Value] = _pos
				if (curMatch2 > matchMinPos)
					if (_bufferBase!![_bufferOffset + curMatch2] == _bufferBase!![cur]) {
						maxLen = 2
						distances[offset++] = maxLen
						distances[offset++] = _pos - curMatch2 - 1
					}
				if (curMatch3 > matchMinPos)
					if (_bufferBase!![_bufferOffset + curMatch3] == _bufferBase!![cur]) {
						if (curMatch3 == curMatch2)
							offset -= 2
						maxLen = 3
						distances[offset++] = maxLen
						distances[offset++] = _pos - curMatch3 - 1
						curMatch2 = curMatch3
					}
				if (offset != 0 && curMatch2 == curMatch) {
					offset -= 2
					maxLen = kStartMaxLen
				}
			}

			_hash[kFixHashSize + hashValue] = _pos

			var ptr0 = (_cyclicBufferPos shl 1) + 1
			var ptr1 = _cyclicBufferPos shl 1

			var len0: Int
			var len1: Int
			len1 = kNumHashDirectBytes
			len0 = len1

			if (kNumHashDirectBytes != 0) {
				if (curMatch > matchMinPos) {
					if (_bufferBase!![_bufferOffset + curMatch + kNumHashDirectBytes] != _bufferBase!![cur + kNumHashDirectBytes]) {
						maxLen = kNumHashDirectBytes
						distances[offset++] = maxLen
						distances[offset++] = _pos - curMatch - 1
					}
				}
			}

			var count = _cutValue

			while (true) {
				if (curMatch <= matchMinPos || count-- == 0) {
					_son[ptr1] = kEmptyHashValue
					_son[ptr0] = _son[ptr1]
					break
				}
				val delta = _pos - curMatch
				val cyclicPos = (if (delta <= _cyclicBufferPos)
					_cyclicBufferPos - delta
				else
					_cyclicBufferPos - delta + _cyclicBufferSize) shl 1

				val pby1 = _bufferOffset + curMatch
				var len = min(len0, len1)
				if (_bufferBase!![pby1 + len] == _bufferBase!![cur + len]) {
					while (++len != lenLimit)
						if (_bufferBase!![pby1 + len] != _bufferBase!![cur + len])
							break
					if (maxLen < len) {
						maxLen = len
						distances[offset++] = maxLen
						distances[offset++] = delta - 1
						if (len == lenLimit) {
							_son[ptr1] = _son[cyclicPos]
							_son[ptr0] = _son[cyclicPos + 1]
							break
						}
					}
				}
				if (_bufferBase!![pby1 + len] and 0xFF < _bufferBase!![cur + len] and 0xFF) {
					_son[ptr1] = curMatch
					ptr1 = cyclicPos + 1
					curMatch = _son[ptr1]
					len1 = len
				} else {
					_son[ptr0] = curMatch
					ptr0 = cyclicPos
					curMatch = _son[ptr0]
					len0 = len
				}
			}
			MovePos()
			return offset
		}

		fun Skip(num: Int) {
			var nnum = num
			do {
				val lenLimit: Int
				if (_pos + _matchMaxLen <= _streamPos)
					lenLimit = _matchMaxLen
				else {
					lenLimit = _streamPos - _pos
					if (lenLimit < kMinMatchCheck) {
						MovePos()
						continue
					}
				}

				val matchMinPos = if (_pos > _cyclicBufferSize) _pos - _cyclicBufferSize else 0
				val cur = _bufferOffset + _pos

				val hashValue: Int

				if (HASH_ARRAY) {
					var temp = CrcTable[_bufferBase!![cur] and 0xFF] xor (_bufferBase!![cur + 1] and 0xFF)
					val hash2Value = temp and kHash2Size - 1
					_hash[hash2Value] = _pos
					temp = temp xor ((_bufferBase!![cur + 2] and 0xFF) shl 8)
					val hash3Value = temp and kHash3Size - 1
					_hash[kHash3Offset + hash3Value] = _pos
					hashValue = temp xor (CrcTable[_bufferBase!![cur + 3] and 0xFF] shl 5) and _hashMask
				} else
					hashValue = _bufferBase!![cur] and 0xFF xor ((_bufferBase!![cur + 1] and 0xFF) shl 8)

				var curMatch = _hash[kFixHashSize + hashValue]
				_hash[kFixHashSize + hashValue] = _pos

				var ptr0 = (_cyclicBufferPos shl 1) + 1
				var ptr1 = _cyclicBufferPos shl 1

				var len0: Int
				var len1: Int
				len1 = kNumHashDirectBytes
				len0 = len1

				var count = _cutValue
				while (true) {
					if (curMatch <= matchMinPos || count-- == 0) {
						_son[ptr1] =
								kEmptyHashValue
						_son[ptr0] = _son[ptr1]
						break
					}

					val delta = _pos - curMatch
					val cyclicPos = (if (delta <= _cyclicBufferPos)
						_cyclicBufferPos - delta
					else
						_cyclicBufferPos - delta + _cyclicBufferSize) shl 1

					val pby1 = _bufferOffset + curMatch
					var len = min(len0, len1)
					if (_bufferBase!![pby1 + len] == _bufferBase!![cur + len]) {
						while (++len != lenLimit)
							if (_bufferBase!![pby1 + len] != _bufferBase!![cur + len])
								break
						if (len == lenLimit) {
							_son[ptr1] = _son[cyclicPos]
							_son[ptr0] = _son[cyclicPos + 1]
							break
						}
					}
					if (_bufferBase!![pby1 + len] and 0xFF < _bufferBase!![cur + len] and 0xFF) {
						_son[ptr1] = curMatch
						ptr1 = cyclicPos + 1
						curMatch = _son[ptr1]
						len1 = len
					} else {
						_son[ptr0] = curMatch
						ptr0 = cyclicPos
						curMatch = _son[ptr0]
						len0 = len
					}
				}
				MovePos()
			} while (--nnum != 0)
		}

		internal fun NormalizeLinks(items: IntArray, numItems: Int, subValue: Int) {
			for (i in 0 until numItems) {
				var value = items[i]
				if (value <= subValue)
					value = kEmptyHashValue
				else
					value -= subValue
				items[i] = value
			}
		}

		internal fun Normalize() {
			val subValue = _pos - _cyclicBufferSize
			NormalizeLinks(_son, _cyclicBufferSize * 2, subValue)
			NormalizeLinks(_hash, _hashSizeSum, subValue)
			ReduceOffsets(subValue)
		}

		fun SetCutValue(cutValue: Int) {
			_cutValue = cutValue
		}

		companion object {
			internal const val kHash2Size = 1 shl 10
			internal const val kHash3Size = 1 shl 16
			internal const val kBT2HashSize = 1 shl 16
			internal const val kStartMaxLen = 1
			internal const val kHash3Offset = kHash2Size
			internal const val kEmptyHashValue = 0
			internal const val kMaxValForNormalize = (1 shl 30) - 1

			private val CrcTable = CRC32.TABLE
		}
	}

	open class LzInWindow {
		var _bufferBase: ByteArray? = null // pointer to buffer with data
		internal var _stream: SyncInputStream? = null
		internal var _posLimit: Int = 0  // offset (from _buffer) of first byte when new block reading must be done
		internal var _streamEndWasReached: Boolean = false // if (true) then _streamPos shows real end of stream

		internal var _pointerToLastSafePosition: Int = 0

		var _bufferOffset: Int = 0

		var _blockSize: Int = 0  // Size of Allocated memory block
		var _pos: Int = 0             // offset (from _buffer) of curent byte
		internal var _keepSizeBefore: Int = 0  // how many BYTEs must be kept in buffer before _pos
		internal var _keepSizeAfter: Int = 0   // how many BYTEs must be kept buffer after _pos
		var _streamPos: Int = 0   // offset (from _buffer) of first not read byte from Stream

		fun MoveBlock() {
			var offset = _bufferOffset + _pos - _keepSizeBefore
			// we need one additional byte, since MovePos moves on 1 byte.
			if (offset > 0)
				offset--

			val numBytes = _bufferOffset + _streamPos - offset

			// check negative offset ????
			for (i in 0 until numBytes)
				_bufferBase!![i] = _bufferBase!![offset + i]
			_bufferOffset -= offset
		}

		fun ReadBlock() {
			if (_streamEndWasReached)
				return
			while (true) {
				val size = 0 - _bufferOffset + _blockSize - _streamPos
				if (size == 0)
					return
				val numReadBytes = _stream!!.read(_bufferBase!!, _bufferOffset + _streamPos, size)
				if (numReadBytes <= 0) {
					_posLimit = _streamPos
					val pointerToPostion = _bufferOffset + _posLimit
					if (pointerToPostion > _pointerToLastSafePosition)
						_posLimit = _pointerToLastSafePosition - _bufferOffset

					_streamEndWasReached = true
					return
				}
				_streamPos += numReadBytes
				if (_streamPos >= _pos + _keepSizeAfter)
					_posLimit = _streamPos - _keepSizeAfter
			}
		}

		internal fun Free() {
			_bufferBase = null
		}

		fun Create(keepSizeBefore: Int, keepSizeAfter: Int, keepSizeReserv: Int) {
			_keepSizeBefore = keepSizeBefore
			_keepSizeAfter = keepSizeAfter
			val blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv
			if (_bufferBase == null || _blockSize != blockSize) {
				Free()
				_blockSize = blockSize
				_bufferBase = ByteArray(_blockSize)
			}
			_pointerToLastSafePosition = _blockSize - keepSizeAfter
		}

		fun SetStream(stream: SyncInputStream) {
			_stream = stream
		}

		fun ReleaseStream() {
			_stream = null
		}

		open fun Init() {
			_bufferOffset = 0
			_pos = 0
			_streamPos = 0
			_streamEndWasReached = false
			ReadBlock()
		}

		open fun MovePos() {
			_pos++
			if (_pos > _posLimit) {
				val pointerToPostion = _bufferOffset + _pos
				if (pointerToPostion > _pointerToLastSafePosition)
					MoveBlock()
				ReadBlock()
			}
		}

		fun GetIndexByte(index: Int): Byte {
			return _bufferBase!![_bufferOffset + _pos + index]
		}

		// index + limit have not to exceed _keepSizeAfter;
		fun GetMatchLen(index: Int, distance: Int, limit: Int): Int {
			var ddis = distance
			var dlim = limit
			if (_streamEndWasReached && _pos + index + dlim > _streamPos) dlim = _streamPos - (_pos + index)
			ddis++
			// Byte *pby = _buffer + (size_t)_pos + index;
			val pby = _bufferOffset + _pos + index

			var i: Int = 0
			while (i < dlim && _bufferBase!![pby + i] == _bufferBase!![pby + i - ddis]) {
				i++
			}
			return i
		}

		fun GetNumAvailableBytes(): Int {
			return _streamPos - _pos
		}

		fun ReduceOffsets(subValue: Int) {
			_bufferOffset += subValue
			_posLimit -= subValue
			_pos -= subValue
			_streamPos -= subValue
		}
	}

	class LzOutWindow {
		internal var _buffer: ByteArray? = null
		internal var _pos: Int = 0
		internal var _windowSize = 0
		internal var _streamPos: Int = 0
		internal var _stream: SyncOutputStream? = null

		fun Create(windowSize: Int) {
			if (_buffer == null || _windowSize != windowSize)
				_buffer = ByteArray(windowSize)
			_windowSize = windowSize
			_pos = 0
			_streamPos = 0
		}

		fun SetStream(stream: SyncOutputStream) {
			ReleaseStream()
			_stream = stream
		}

		fun ReleaseStream() {
			Flush()
			_stream = null
		}

		fun Init(solid: Boolean) {
			if (!solid) {
				_streamPos = 0
				_pos = 0
			}
		}

		fun Flush() {
			val size = _pos - _streamPos
			if (size == 0)
				return
			_stream!!.write(_buffer!!, _streamPos, size)
			if (_pos >= _windowSize)
				_pos = 0
			_streamPos = _pos
		}

		fun CopyBlock(distance: Int, len: Int) {
			var llen = len
			var pos = _pos - distance - 1
			if (pos < 0)
				pos += _windowSize
			while (llen != 0) {
				if (pos >= _windowSize)
					pos = 0
				_buffer!![_pos++] = _buffer!![pos++]
				if (_pos >= _windowSize)
					Flush()
				llen--
			}
		}

		fun PutByte(b: Byte) {
			_buffer!![_pos++] = b
			if (_pos >= _windowSize)
				Flush()
		}

		fun GetByte(distance: Int): Byte {
			var pos = _pos - distance - 1
			if (pos < 0)
				pos += _windowSize
			return _buffer!![pos]
		}
	}
}

private infix fun Byte.and(mask: Int): Int = this.toInt() and mask
private infix fun Byte.shl(that: Int): Int = this.toInt() shl that
private infix fun Byte.shr(that: Int): Int = this.toInt() shr that
