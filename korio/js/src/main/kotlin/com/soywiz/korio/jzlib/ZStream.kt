/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2000-2011 ymnk, JCraft,Inc. All rights reserved.

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
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.soywiz.korio.jzlib

import com.soywiz.korio.lang.System

/**
 * ZStream
 *
 * deprecated  Not for public use in the future.
 */
//@Deprecated
open class ZStream constructor(var adler: Checksum = Adler32()) {

	var next_in: ByteArray? = null     // next input byte
	var next_in_index: Int = 0
	var avail_in: Int = 0       // number of bytes available at next_in
	var total_in: Long = 0      // total nb of input bytes read so far

	var next_out: ByteArray? = null    // next output byte should be put there
	var next_out_index: Int = 0
	var avail_out: Int = 0      // remaining free space at next_out
	var total_out: Long = 0     // total nb of bytes output so far

	var msg: String? = null

	internal var dstate: Deflate? = null
	internal var istate: Inflate? = null

	internal var data_type: Int = 0 // best guess about the data type: ascii or binary
	fun inflateInit(nowrap: Boolean): Int {
		return inflateInit(DEF_WBITS, nowrap)
	}

	fun inflateInit(wrapperType: JZlib.WrapperType): Int {
		return inflateInit(DEF_WBITS, wrapperType)
	}

	fun inflateInit(w: Int, wrapperType: JZlib.WrapperType): Int {
		var w = w
		var nowrap = false
		if (wrapperType === JZlib.W_NONE) {
			nowrap = true
		} else if (wrapperType === JZlib.W_GZIP) {
			w += 16
		} else if (wrapperType === JZlib.W_ANY) {
			w = w or Inflate.INFLATE_ANY
		} else if (wrapperType === JZlib.W_ZLIB) {
		}
		return inflateInit(w, nowrap)
	}

	fun inflateInit(w: Int = DEF_WBITS, nowrap: Boolean = false): Int {
		istate = Inflate(this)
		return istate!!.inflateInit(if (nowrap) -w else w)
	}

	fun inflate(f: Int): Int {
		return if (istate == null) Z_STREAM_ERROR else istate!!.inflate(f)
	}

	fun inflateEnd(): Int {
		return if (istate == null) Z_STREAM_ERROR else istate!!.inflateEnd()
//    istate = null;
	}

	fun inflateSync(): Int {
		return if (istate == null) Z_STREAM_ERROR else istate!!.inflateSync()
	}

	fun inflateSyncPoint(): Int {
		return if (istate == null) Z_STREAM_ERROR else istate!!.inflateSyncPoint()
	}

	fun inflateSetDictionary(dictionary: ByteArray, dictLength: Int): Int {
		return if (istate == null) Z_STREAM_ERROR else istate!!.inflateSetDictionary(dictionary, 0, dictLength)
	}

	fun inflateFinished(): Boolean {
		return istate!!.mode === 12 /*DONE*/
	}

	fun deflateInit(level: Int, nowrap: Boolean): Int {
		return deflateInit(level, MAX_WBITS, nowrap)
	}

	fun deflateInit(level: Int, bits: Int, memlevel: Int, wrapperType: JZlib.WrapperType): Int {
		var bits = bits
		if (bits < 9 || bits > 15) {
			return Z_STREAM_ERROR
		}
		if (wrapperType === JZlib.W_NONE) {
			bits *= -1
		} else if (wrapperType === JZlib.W_GZIP) {
			bits += 16
		} else if (wrapperType === JZlib.W_ANY) {
			return Z_STREAM_ERROR
		} else if (wrapperType === JZlib.W_ZLIB) {
		}
		return this.deflateInit(level, bits, memlevel)
	}

	fun deflateInit(level: Int, bits: Int, memlevel: Int): Int {
		dstate = Deflate(this)
		return dstate!!.deflateInit(level, bits, memlevel)
	}

	fun deflateInit(level: Int, bits: Int = MAX_WBITS, nowrap: Boolean = false): Int {
		dstate = Deflate(this)
		return dstate!!.deflateInit(level, if (nowrap) -bits else bits)
	}

	open fun deflate(flush: Int): Int {
		return if (dstate == null) {
			Z_STREAM_ERROR
		} else dstate!!.deflate(flush)
	}

	fun deflateEnd(): Int {
		if (dstate == null) return Z_STREAM_ERROR
		val ret = dstate!!.deflateEnd()
		dstate = null
		return ret
	}

	fun deflateParams(level: Int, strategy: Int): Int {
		return if (dstate == null) Z_STREAM_ERROR else dstate!!.deflateParams(level, strategy)
	}

	fun deflateSetDictionary(dictionary: ByteArray, dictIndex: Int, dictLength: Int): Int {
		return if (dstate == null) Z_STREAM_ERROR else dstate!!.deflateSetDictionary(dictionary, dictIndex, dictLength)
	}

	// Flush as much pending output as possible. All deflate() output goes
	// through this function so some applications may wish to modify it
	// to avoid allocating a large strm->next_out buffer and copying into it.
	// (See also read_buf()).
	internal fun flush_pending() {
		var len = dstate!!.pending

		if (len > avail_out) len = avail_out
		if (len == 0) return

		if (dstate!!.pending_buf.size <= dstate!!.pending_out ||
			next_out!!.size <= next_out_index ||
			dstate!!.pending_buf.size < dstate!!.pending_out + len ||
			next_out!!.size < next_out_index + len) {
			//System.out.println(dstate.pending_buf.length+", "+dstate.pending_out+
			//		 ", "+next_out.length+", "+next_out_index+", "+len);
			//System.out.println("avail_out="+avail_out);
		}

		System.arraycopy(dstate!!.pending_buf, dstate!!.pending_out,
			next_out!!, next_out_index, len)

		next_out_index += len
		dstate!!.pending_out += len
		total_out += len.toLong()
		avail_out -= len
		dstate!!.pending -= len
		if (dstate!!.pending === 0) {
			dstate!!.pending_out = 0
		}
	}

	// Read a new buffer from the current input stream, update the adler32
	// and total number of bytes read.  All deflate() input goes through
	// this function so some applications may wish to modify it to avoid
	// allocating a large strm->next_in buffer and copying from it.
	// (See also flush_pending()).
	internal fun read_buf(buf: ByteArray, start: Int, size: Int): Int {
		var len = avail_in

		if (len > size) len = size
		if (len == 0) return 0

		avail_in -= len

		if (dstate!!.wrap !== 0) {
			adler.update(next_in!!, next_in_index, len)
		}
		System.arraycopy(next_in!!, next_in_index, buf, start, len)
		next_in_index += len
		total_in += len.toLong()
		return len
	}

	fun getAdler(): Int {
		return adler.value
	}

	fun free() {
		next_in = null
		next_out = null
		msg = null
	}

	fun setOutput(buf: ByteArray, off: Int = 0, len: Int = buf.size) {
		next_out = buf
		next_out_index = off
		avail_out = len
	}

	fun setInput(buf: ByteArray, append: Boolean) {
		setInput(buf, 0, buf.size, append)
	}

	fun setInput(buf: ByteArray, off: Int = 0, len: Int = buf.size, append: Boolean = false) {
		if (len <= 0 && append && next_in != null) return

		if (avail_in > 0 && append) {
			val tmp = ByteArray(avail_in + len)
			System.arraycopy(next_in!!, next_in_index, tmp, 0, avail_in)
			System.arraycopy(buf, off, tmp, avail_in, len)
			next_in = tmp
			next_in_index = 0
			avail_in += len
		} else {
			next_in = buf
			next_in_index = off
			avail_in = len
		}
	}

	open fun end(): Int {
		return Z_OK
	}

	open fun finished(): Boolean {
		return false
	}

	companion object {

		private val MAX_WBITS = 15        // 32K LZ77 window
		private val DEF_WBITS = MAX_WBITS

		private val Z_NO_FLUSH = 0
		private val Z_PARTIAL_FLUSH = 1
		private val Z_SYNC_FLUSH = 2
		private val Z_FULL_FLUSH = 3
		private val Z_FINISH = 4

		private val MAX_MEM_LEVEL = 9

		private val Z_OK = 0
		private val Z_STREAM_END = 1
		private val Z_NEED_DICT = 2
		private val Z_ERRNO = -1
		private val Z_STREAM_ERROR = -2
		private val Z_DATA_ERROR = -3
		private val Z_MEM_ERROR = -4
		private val Z_BUF_ERROR = -5
		private val Z_VERSION_ERROR = -6
	}
}
