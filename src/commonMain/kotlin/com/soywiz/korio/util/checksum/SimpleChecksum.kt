package com.soywiz.korio.util.checksum

import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

interface SimpleChecksum {
	val INITIAL: Int
	fun update(old: Int, data: ByteArray, offset: Int = 0, len: Int = data.size - offset): Int

	companion object {
		val DUMMY = object : SimpleChecksum {
			override val INITIAL: Int = 0
			override fun update(old: Int, data: ByteArray, offset: Int, len: Int): Int = 0
		}
	}
}

fun SimpleChecksum.compute(data: ByteArray, offset: Int = 0, len: Int = data.size - offset) = update(INITIAL, data, offset, len)

fun ByteArray.checksum(checksum: SimpleChecksum): Int = checksum.compute(this)

fun SyncInputStream.checksum(checksum: SimpleChecksum): Int {
	var value = checksum.INITIAL
	val temp = ByteArray(1024)

	while (true) {
		val read = this.read(temp)
		if (read <= 0) break
		value = checksum.update(value, temp, 0, read)
	}

	return value
}

suspend fun AsyncInputStream.checksum(checksum: SimpleChecksum): Int {
	var value = checksum.INITIAL
	val temp = ByteArray(1024)

	while (true) {
		val read = this.read(temp)
		if (read <= 0) break
		value = checksum.update(value, temp, 0, read)
	}

	return value
}

suspend fun AsyncInputOpenable.checksum(checksum: SimpleChecksum) = this.openRead().use { this.checksum(checksum) }
