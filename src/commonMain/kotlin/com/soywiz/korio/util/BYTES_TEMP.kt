package com.soywiz.korio.util

import com.soywiz.kds.*
import com.soywiz.korio.lang.*

const val BYTES_TEMP_SIZE = 0x10000

@PublishedApi
internal val BYTES_EMPTY = byteArrayOf()

@PublishedApi
internal val BYTES_TEMP by threadLocal { ByteArray(BYTES_TEMP_SIZE) }

@PublishedApi
internal val smallBytesPool by threadLocal { Pool(preallocate = 16) { ByteArray(16) } }

@PublishedApi
internal inline fun <T, R> Pool<T>.alloc2(callback: (T) -> R): R {
	val temp = alloc()
	try {
		return callback(temp)
	} finally {
		free(temp)
	}
}

@PublishedApi
internal inline fun <T, R> Pool<T>.allocThis(callback: T.() -> R): R {
	val temp = alloc()
	try {
		return callback(temp)
	} finally {
		free(temp)
	}
}
