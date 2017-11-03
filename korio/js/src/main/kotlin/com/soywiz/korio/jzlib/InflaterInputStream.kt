/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2011 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.soywiz.korio.jzlib

import com.soywiz.korio.EOFException
import com.soywiz.korio.IOException
import com.soywiz.korio.lang.System
import com.soywiz.korio.lang.and
import kotlin.math.min

open class InflaterInputStream(
	`in`: InputStream,
	val inflater: Inflater,
	size: Int = DEFAULT_BUFSIZE,
	private val close_in: Boolean = true
) : FilterInputStream(`in`) {
	protected var buf: ByteArray = ByteArray(size)

	private var closed = false

	private var eof = false

	protected var myinflater = false

	private val byte1 = ByteArray(1)

	private val b = ByteArray(512)

	val total_in: Long
		get() = inflater.total_in

	val total_out: Long
		get() = inflater.total_out

	val availIn: ByteArray?
		get() {
			if (inflater.avail_in <= 0) return null
			val tmp = ByteArray(inflater.avail_in)
			System.arraycopy(inflater.next_in!!, inflater.next_in_index, tmp, 0, inflater.avail_in)
			return tmp
		}

	constructor(`in`: InputStream, nowrap: Boolean = false) : this(`in`, Inflater(nowrap)) {
		myinflater = true
	}

	override fun read(): Int {
		if (closed) {
			throw IOException("Stream closed")
		}
		return if (read(byte1, 0, 1) == -1) -1 else byte1[0] and 0xff
	}

	override fun read(b: ByteArray, off: Int, len: Int): Int {
		var off = off
		if (closed) {
			throw IOException("Stream closed")
		}
		if (b == null) {
			throw NullPointerException()
		} else if (off < 0 || len < 0 || len > b.size - off) {
			throw IndexOutOfBoundsException()
		} else if (len == 0) {
			return 0
		} else if (eof) {
			return -1
		}

		var n = 0
		inflater.setOutput(b, off, len)
		while (!eof) {
			if (inflater.avail_in === 0)
				fill()
			val err = inflater.inflate(JZlib.Z_NO_FLUSH)
			n += inflater.next_out_index - off
			off = inflater.next_out_index
			when (err) {
				JZlib.Z_DATA_ERROR -> throw IOException(inflater.msg ?: "")
				JZlib.Z_STREAM_END, JZlib.Z_NEED_DICT -> {
					eof = true
					if (err == JZlib.Z_NEED_DICT)
						return -1
				}
			}
			if (inflater.avail_out === 0)
				break
		}
		return n
	}

	override fun available(): Int {
		if (closed) {
			throw IOException("Stream closed")
		}
		return if (eof) {
			0
		} else {
			1
		}
	}

	override fun skip(n: Long): Long {
		if (n < 0) {
			throw IllegalArgumentException("negative skip length")
		}

		if (closed) {
			throw IOException("Stream closed")
		}

		val max = min(n, Int.MAX_VALUE.toLong()).toInt()
		var total = 0
		while (total < max) {
			var len = max - total
			if (len > b.size) {
				len = b.size
			}
			len = read(b, 0, len)
			if (len == -1) {
				eof = true
				break
			}
			total += len
		}
		return total.toLong()
	}

	override fun close() {
		if (!closed) {
			if (myinflater)
				inflater.end()
			if (close_in)
				`in`.close()
			closed = true
		}
	}

	protected fun fill() {
		if (closed) {
			throw IOException("Stream closed")
		}
		var len = `in`.read(buf, 0, buf.size)
		if (len == -1) {
			if (inflater.istate!!.wrap == 0 && !inflater.finished()) {
				buf[0] = 0
				len = 1
			} else if (inflater.istate!!.was != -1) {  // in reading trailer
				throw IOException("footer is not found")
			} else {
				throw EOFException("Unexpected end of ZLIB input stream")
			}
		}
		inflater.setInput(buf, 0, len, true)
	}

	override fun markSupported(): Boolean {
		return false
	}

	@Synchronized override fun mark(readlimit: Int) {}

	@Synchronized
	override fun reset() {
		throw IOException("mark/reset not supported")
	}

	open fun readHeader() {

		val empty = byteArrayOf()
		inflater.setInput(empty, 0, 0, false)
		inflater.setOutput(empty, 0, 0)

		var err = inflater.inflate(JZlib.Z_NO_FLUSH)
		if (!inflater.istate!!.inParsingHeader()) {
			return
		}

		val b1 = ByteArray(1)
		do {
			val i = `in`.read(b1)
			if (i <= 0)
				throw IOException("no input")
			inflater.setInput(b1)
			err = inflater.inflate(JZlib.Z_NO_FLUSH)
			if (err != 0/*Z_OK*/)
				throw IOException(inflater.msg ?: "")
		} while (inflater.istate!!.inParsingHeader())
	}

	companion object {

		protected val DEFAULT_BUFSIZE = 512
	}
}
