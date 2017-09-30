package com.soywiz.korio.mem

expect class FastMemory {
	companion object {
		fun alloc(size: Int): FastMemory
		fun copy(src: FastMemory, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit
	}

	val size: Int

	operator fun get(index: Int): Int
	operator fun set(index: Int, value: Int): Unit

	fun setAlignedInt16(index: Int, value: Short): Unit
	fun getAlignedInt16(index: Int): Short

	fun setAlignedInt32(index: Int, value: Int): Unit
	fun getAlignedInt32(index: Int): Int

	fun setAlignedFloat32(index: Int, value: Float): Unit
	fun getAlignedFloat32(index: Int): Float

	fun setAlignedArrayInt8(index: Int, data: ByteArray, offset: Int, len: Int)
	fun setAlignedArrayInt16(index: Int, data: ShortArray, offset: Int, len: Int)
	fun setAlignedArrayInt32(index: Int, data: IntArray, offset: Int, len: Int)
	fun setAlignedArrayFloat32(index: Int, data: FloatArray, offset: Int, len: Int)
}