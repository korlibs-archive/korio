package com.soywiz.korio.compat

// Required to work on Gradle 4.7 that uses Kotlin 1.2.20
fun ByteArray.copyOfRangeCompat(start: Int, end: Int): ByteArray {
	val out = ByteArray(end - start)
	for (n in 0 until out.size) out[n] = this[start + n]
	return out
}
