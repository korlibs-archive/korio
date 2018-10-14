package com.soywiz.korio.lang

import org.khronos.webgl.*

actual val UTF8: Charset = object : UTC8CharsetBase("UTF-8") {
	val textDecoder: TextDecoder? = try { TextDecoder("utf-8") } catch (e: dynamic) { null } // Do not fail if not supported!

	override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int) {
		if (textDecoder != null) {
			val srcBuffer = src.unsafeCast<Int8Array>()
			out.append(textDecoder.decode(Int8Array(srcBuffer.buffer, start, end - start)))
		} else {
			super.decode(out, src, start, end)
		}
	}
}

external class TextDecoder(charset: String) {
	fun decode(data: ArrayBufferView): String
}
