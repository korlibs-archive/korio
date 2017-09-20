package com.soywiz.korio.lang

class Bytes(val byteArray: ByteArray) {
	override fun equals(other: Any?): Boolean {
		return when {
			this === other -> true
			other is Bytes -> this.byteArray.contentEquals(other.byteArray)
			else -> false
		}
	}

	override fun hashCode(): Int = byteArray.contentHashCode()

	override fun toString(): String = byteArray.contentToString()

	fun toByteArray() = byteArray
}

fun ByteArray.toBytes() = Bytes(this)