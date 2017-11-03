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

import com.soywiz.korio.IOException

class GZIPInputStream
constructor(`in`: InputStream,
			inflater: Inflater,
			size: Int,
			close_in: Boolean) : InflaterInputStream(`in`, inflater, size, close_in) {

	val modifiedtime: Long
		get() = inflater!!.istate!!.gzipHeader!!.modifiedTime

	val os: Int
		get() = inflater!!.istate!!.gzipHeader!!.getOS()

	val name: String
		get() = inflater!!.istate!!.gzipHeader!!.getName()

	val comment: String
		get() = inflater!!.istate!!.gzipHeader!!.getComment()

	/*DONE*/ val crc: Long
		get() {
			if (inflater!!.istate!!.mode != 12)
				throw GZIPException("checksum is not calculated yet.")
			return inflater.istate!!.gzipHeader!!.crc
		}

	constructor(`in`: InputStream, size: Int = InflaterInputStream.Companion.DEFAULT_BUFSIZE, close_in: Boolean = true)
		: this(`in`, Inflater(15 + 16), size, close_in) {
		myinflater = true
	}

	override fun readHeader() {

		val empty = byteArrayOf()
		inflater!!.setOutput(empty, 0, 0)
		inflater.setInput(empty, 0, 0, false)

		val b = ByteArray(10)

		var n = fill(b)
		if (n != 10) {
			if (n > 0) {
				inflater.setInput(b, 0, n, false)
				//inflater.next_in_index = n;
				inflater.next_in_index = 0
				inflater.avail_in = n
			}
			throw IOException("no input")
		}

		inflater.setInput(b, 0, n, false)

		val b1 = ByteArray(1)
		do {
			if (inflater.avail_in <= 0) {
				val i = `in`.read(b1)
				if (i <= 0)
					throw IOException("no input")
				inflater.setInput(b1, 0, 1, true)
			}

			val err = inflater.inflate(JZlib.Z_NO_FLUSH)

			if (err != 0/*Z_OK*/) {
				val len = 2048 - inflater.next_in!!.size
				if (len > 0) {
					val tmp = ByteArray(len)
					n = fill(tmp)
					if (n > 0) {
						inflater.avail_in += inflater.next_in_index
						inflater.next_in_index = 0
						inflater.setInput(tmp, 0, n, true)
					}
				}
				//inflater.next_in_index = inflater.next_in.length;
				inflater.avail_in += inflater.next_in_index
				inflater.next_in_index = 0
				throw IOException(inflater!!.msg!!)
			}
		} while (inflater.istate!!.inParsingHeader())
	}

	private fun fill(buf: ByteArray): Int {
		val len = buf.size
		var n = 0
		do {
			var i = -1
			try {
				i = `in`.read(buf, n, buf.size - n)
			} catch (e: IOException) {
			}

			if (i == -1) {
				break
			}
			n += i
		} while (n < len)
		return n
	}
}