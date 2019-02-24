package com.soywiz.korio.internal

internal inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[n++])
	}
}

internal inline fun <T> Array<T>.fastForEach(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[n++])
	}
}

internal inline fun <T> List<T>.fastForEachReverse(callback: (T) -> Unit) {
	var n = 0
	while (n < size) {
		callback(this[size - n - 1])
		n++
	}
}

internal inline fun <T> ArrayList<T>.fastIterateRemove(callback: (T) -> Boolean): ArrayList<T> {
	var n = 0
	var m = 0
	while (n < size) {
		if (m >= 0 && m != n) this[m] = this[n]
		if (callback(this[n])) m--
		n++
		m++
	}
	while (this.size > m) this.removeAt(this.size - 1)
	return this
}
