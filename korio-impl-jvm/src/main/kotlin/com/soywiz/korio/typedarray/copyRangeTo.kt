package com.soywiz.korio.typedarray

impl fun ByteArray.copyRangeTo(srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
	System.arraycopy(this, srcPos, dst, dstPos, count)
}