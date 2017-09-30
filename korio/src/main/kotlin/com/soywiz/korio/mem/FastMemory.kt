package com.soywiz.korio.mem

import java.nio.ByteBuffer

actual class FastMemory(val buffer: ByteBuffer, actual val size: Int) {
	val i16 = buffer.asShortBuffer()
	val i32 = buffer.asIntBuffer()
	val f32 = buffer.asFloatBuffer()

	companion actual object {
		actual fun alloc(size: Int): FastMemory {
			return FastMemory(ByteBuffer.allocate(size), size)
		}
		actual fun copy(src: FastMemory, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit {
			//dst.buffer.slice()
			//dst.buffer.position(srcPos)
			//dst.buffer.put(src.buffer, srcPos, length)

			// COPY
			for (n in 0 until length) dst[dstPos + n] = src[srcPos + n]
		}
	}
	actual operator fun get(index: Int): Int {
		return buffer.get(index).toInt() and 0xFF
	}
	actual operator fun set(index: Int, value: Int): Unit {
		buffer.put(index, value.toByte())
	}
	actual fun setAlignedInt16(index: Int, value: Short): Unit = run { i16.put(index, value) }
	actual fun getAlignedInt16(index: Int): Short = i16.get(index)
	actual fun setAlignedInt32(index: Int, value: Int): Unit = run { i32.put(index, value) }
	actual fun getAlignedInt32(index: Int): Int = i32.get(index)
	actual fun setAlignedFloat32(index: Int, value: Float): Unit = run { f32.put(index, value) }
	actual fun getAlignedFloat32(index: Int): Float = f32.get(index)

	actual fun setAlignedArrayInt8(index: Int, data: ByteArray, offset: Int, len: Int) {
		for (n in 0 until len) set(index + n, data[offset + n].toInt() and 0xFF)
	}
	actual fun setAlignedArrayInt16(index: Int, data: ShortArray, offset: Int, len: Int) {
		for (n in 0 until len) setAlignedInt16(index + n, data[offset + n])
	}
	actual fun setAlignedArrayInt32(index: Int, data: IntArray, offset: Int, len: Int) {
		for (n in 0 until len) setAlignedInt32(index + n, data[offset + n])
	}
	actual fun setAlignedArrayFloat32(index: Int, data: FloatArray, offset: Int, len: Int) {
		for (n in 0 until len) setAlignedFloat32(index + n, data[offset + n])
	}
}