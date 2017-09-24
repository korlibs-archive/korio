package com.soywiz.korio.typedarray

import java.util.*

impl fun ByteArray.fill(value: Byte, from: Int, to: Int) {
	Arrays.fill(this, from, to, value)
}