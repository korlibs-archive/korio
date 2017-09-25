package kotlinx.arraybuffers

class ArrayBuffer(val byteLength: Int) {
	val data: ByteArray = ByteArray(byteLength)
}

open class ArrayView(val buffer: ArrayBuffer, val byteOffset: Int, val byteLength: Int) {
}

class Int8Array(buffer: ArrayBuffer, byteOffset: Int, byteLength: Int) : ArrayView(buffer, byteOffset, byteLength) {
	operator fun get(index: Int): Int = TODO()
	operator fun set(index: Int, value: Int): Unit = TODO()
}
