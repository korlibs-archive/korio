package com.soywiz.korio.mem

import org.khronos.webgl.*

actual class FastMemory(val buffer: Uint8Array, actual val size: Int) {
	val data = DataView(buffer.buffer)
	val i16 = Int16Array(buffer.buffer)
	val i32 = Int32Array(buffer.buffer)
	val f32 = Float32Array(buffer.buffer)

	companion actual object {
		actual fun alloc(size: Int): FastMemory {
			return FastMemory(Uint8Array((size + 0xF) and 0xF.inv()), size)
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
		return buffer[index].toInt() and 0xFF
	}

	actual operator fun set(index: Int, value: Int): Unit {
		buffer[index] = value.toByte()
	}
	actual fun setAlignedInt16(index: Int, value: Short) = run { i16[index] = value }
	actual fun getAlignedInt16(index: Int): Short = i16[index]
	actual fun setAlignedInt32(index: Int, value: Int) = run { i32[index] = value }
	actual fun getAlignedInt32(index: Int): Int = i32[index]
	actual fun setAlignedFloat32(index: Int, value: Float): Unit = run { f32[index] = value }
	actual fun getAlignedFloat32(index: Int): Float = f32[index]

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

	actual fun getInt16(index: Int): Short = data.getInt16(index)
	actual fun getInt32(index: Int): Int = data.getInt32(index)
	actual fun getFloat32(index: Int): Float = data.getFloat32(index)
}