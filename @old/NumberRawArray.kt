package com.soywiz.korio.ds

import com.soywiz.kds.*
import com.soywiz.kmem.*

class BoolArrayLike(val raw: NumberRawArray) {
	val size: Int get() = raw.size
	operator fun get(index: Int) = raw.getBool(index)
	operator fun set(index: Int, value: Boolean) = raw.setBool(index, value)
}

class ByteArrayLike(val raw: NumberRawArray) {
	val size: Int get() = raw.size
	operator fun get(index: Int) = raw.getByte(index)
	operator fun set(index: Int, value: Byte) = raw.setByte(index, value)
}

class ShortArrayLike(val raw: NumberRawArray) {
	val size: Int get() = raw.size
	operator fun get(index: Int) = raw.getShort(index)
	operator fun set(index: Int, value: Short) = raw.setShort(index, value)
}

class IntArrayLike(val raw: NumberRawArray) {
	val size: Int get() = raw.size
	operator fun get(index: Int) = raw.getInt(index)
	operator fun set(index: Int, value: Int) = raw.setInt(index, value)
}

class FloatArrayLike(val raw: NumberRawArray) {
	val size: Int get() = raw.size
	operator fun get(index: Int) = raw.getFloat(index)
	operator fun set(index: Int, value: Float) = raw.setFloat(index, value)
}

class DoubleArrayLike(val raw: NumberRawArray) {
	val size: Int get() = raw.size
	operator fun get(index: Int) = raw.getDouble(index)
	operator fun set(index: Int, value: Double) = raw.setDouble(index, value)
}

fun NumberRawArray.asBool() = BoolArrayLike(this)
fun NumberRawArray.asByte() = ByteArrayLike(this)
fun NumberRawArray.asShort() = ShortArrayLike(this)
fun NumberRawArray.asInt() = IntArrayLike(this)
fun NumberRawArray.asFloat() = FloatArrayLike(this)
fun NumberRawArray.asDouble() = DoubleArrayLike(this)

interface NumberRawArray {
	val size: Int
	fun getDouble(index: Int): Double
	fun setDouble(index: Int, value: Double)
	fun getBool(index: Int): Boolean = this.getDouble(index) != 0.0
	fun setBool(index: Int, value: Boolean) = this.setDouble(index, value.toInt().toDouble())
	fun getByte(index: Int): Byte = this.getDouble(index).toByte()
	fun setByte(index: Int, value: Byte) = this.setDouble(index, value.toDouble())
	fun getShort(index: Int): Short = this.getDouble(index).toShort()
	fun setShort(index: Int, value: Short) = this.setDouble(index, value.toDouble())
	fun getInt(index: Int): Int = this.getDouble(index).toInt()
	fun setInt(index: Int, value: Int) = this.setDouble(index, value.toDouble())
	fun getFloat(index: Int): Float = this.getDouble(index).toFloat()
	fun setFloat(index: Int, value: Float) = this.setDouble(index, value.toDouble())
	fun getLong(index: Int): Long = this.getDouble(index).toLong()
	fun setLong(index: Int, value: Long) = this.setDouble(index, value.toDouble())
}


class BoolRaw(val v: BooleanArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toInt().toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value != 0.0 }
}

private class ByteRaw(val v: ByteArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toInt().toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toByte() }
}

private class ShortRaw(val v: ShortArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toInt().toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toShort() }
}

private class CharRaw(val v: CharArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toInt().toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toChar() }
}

private class IntRaw(val v: IntArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toInt() }
}

private class FloatRaw(val v: FloatArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toFloat() }
}

private class DoubleRaw(val v: DoubleArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index]
	override fun setDouble(index: Int, value: Double) = run { v[index] = value }
}

private class LongRaw(val v: LongArray) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toLong() }
	override fun getLong(index: Int): Long = v[index]
	override fun setLong(index: Int, value: Long) = run { v[index] = value }
}

private class DoubleListRaw(val v: DoubleArrayList) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index]
	override fun setDouble(index: Int, value: Double) = run { v[index] = value }
}

private class IntListRaw(val v: IntArrayList) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toInt() }
}

private class NumberList(val v: List<Number>) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { (v as MutableList<Double>)[index] = value }
}

private class Int8BufferRaw(val v: Int8Buffer) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toByte() }
}

private class Int16BufferRaw(val v: Int16Buffer) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toShort() }
}

private class Int32BufferRaw(val v: Int32Buffer) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toInt() }
}

private class Float32BufferRaw(val v: Float32Buffer) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index].toDouble()
	override fun setDouble(index: Int, value: Double) = run { v[index] = value.toFloat() }
}

private class Float64BufferRaw(val v: Float64Buffer) : NumberRawArray {
	override val size get() = v.size
	override fun getDouble(index: Int): Double = v[index]
	override fun setDouble(index: Int, value: Double) = run { v[index] = value }
}

// Buffers
val Uint8Buffer.raw: NumberRawArray get() = Int8BufferRaw(this.b)
val Uint16Buffer.raw: NumberRawArray get() = Int16BufferRaw(this.b)
val Int8Buffer.raw: NumberRawArray get() = Int8BufferRaw(this)
val Int16Buffer.raw: NumberRawArray get() = Int16BufferRaw(this)
val Int32Buffer.raw: NumberRawArray get() = Int32BufferRaw(this)
val Float32Buffer.raw: NumberRawArray get() = Float32BufferRaw(this)
val Float64Buffer.raw: NumberRawArray get() = Float64BufferRaw(this)

// Arrays
val BooleanArray.raw: NumberRawArray get() = BoolRaw(this)
val ByteArray.raw: NumberRawArray get() = ByteRaw(this)
val ShortArray.raw: NumberRawArray get() = ShortRaw(this)
val CharArray.raw: NumberRawArray get() = CharRaw(this)
val IntArray.raw: NumberRawArray get() = IntRaw(this)
val FloatArray.raw: NumberRawArray get() = FloatRaw(this)
val DoubleArray.raw: NumberRawArray get() = DoubleRaw(this)
val LongArray.raw: NumberRawArray get() = LongRaw(this)

// Lists
val IntArrayList.raw: NumberRawArray get() = IntListRaw(this)
val DoubleArrayList.raw: NumberRawArray get() = DoubleListRaw(this)
val List<Number>.raw: NumberRawArray get() = NumberList(this)

fun NumberRawArray(any: Any?): NumberRawArray {
	return when (any) {
		is Uint8Buffer -> any.raw
		is Uint16Buffer -> any.raw
		is Int8Buffer -> any.raw
		is Int16Buffer -> any.raw
		is Int32Buffer -> any.raw
		is Float32Buffer -> any.raw
		is Float64Buffer -> any.raw

		is BooleanArray -> any.raw
		is ByteArray -> any.raw
		is ShortArray -> any.raw
		is CharArray -> any.raw
		is IntArray -> any.raw
		is FloatArray -> any.raw
		is DoubleArray -> any.raw
		is LongArray -> any.raw
		is IntArrayList -> any.raw
		is DoubleArrayList -> any.raw
		is List<*> -> (any as List<Number>).raw
		else -> error("$any is not a supported array")
	}
}