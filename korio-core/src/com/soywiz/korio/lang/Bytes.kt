package com.soywiz.korio.lang

import java.util.*

class Bytes(val byteArray: ByteArray) {
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other?.javaClass != javaClass) return false

		other as Bytes

		if (!Arrays.equals(byteArray, other.byteArray)) return false

		return true
	}

	override fun hashCode(): Int = Arrays.hashCode(byteArray)

	override fun toString(): String = Arrays.toString(byteArray)

	fun toByteArray() = byteArray
}

fun ByteArray.toBytes() = Bytes(this)