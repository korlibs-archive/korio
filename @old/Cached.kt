package com.soywiz.korio.util

class CachedInt(val initial: Int) {
	var current = initial
	inline operator fun invoke(value: Int, callback: () -> Unit) {
		if (current != value) {
			current = value
			callback()
		}
	}
}

class CachedInt2(val i1: Int, val i2: Int) {
	var c1 = i1
	var c2 = i2
	inline operator fun invoke(i1: Int, i2: Int, callback: () -> Unit) {
		if (c1 != i1 || c2 != i2) {
			c1 = i1
			c2 = i2
			callback()
		}
	}
}

class CachedInt4(val i1: Int, val i2: Int, val i3: Int, val i4: Int) {
	var c1 = i1
	var c2 = i2
	var c3 = i3
	var c4 = i4
	inline operator fun invoke(i1: Int, i2: Int, i3: Int, i4: Int, callback: () -> Unit) {
		if (c1 != i1 || c2 != i2 || c3 != i3 || c4 != i4) {
			c1 = i1
			c2 = i2
			c3 = i3
			c4 = i4
			callback()
		}
	}
}

class CachedFloat(val initial: Float) {
	var current = initial
	inline operator fun invoke(value: Float, callback: () -> Unit) {
		if (current != value) {
			current = value
			callback()
		}
	}
}

class CachedFloat2(val i1: Float, val i2: Float) {
	var c1 = i1
	var c2 = i2
	inline operator fun invoke(i1: Float, i2: Float, callback: () -> Unit) {
		if (c1 != i1 || c2 != i2) {
			c1 = i1
			c2 = i2
			callback()
		}
	}
}

class CachedDouble(val initial: Double) {
	var current = initial
	inline operator fun invoke(value: Double, callback: () -> Unit) {
		if (current != value) {
			current = value
			callback()
		}
	}
}

class CachedObject<T>(val initial: T) {
	var current = initial
	inline operator fun invoke(value: T, callback: () -> Unit) {
		if (current != value) {
			current = value
			callback()
		}
	}
}
