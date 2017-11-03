/* -*-mode:java; c-basic-offset:2; -*- */
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
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.soywiz.korio.jzlib

import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.System
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.lang.toString

/**
 * @see "http://www.ietf.org/rfc/rfc1952.txt"
 */
class GZIPHeader {

	val ISO_8859_1 = Charsets.ISO_8859_1

	internal var text = false
	private val fhcrc = false
	internal var time: Long = 0
	internal var xflags: Int = 0
	internal var os = 255
	internal var extra: ByteArray? = null
	internal var name: ByteArray? = null
	internal var comment: ByteArray? = null
	internal var hcrc: Int = 0
	var crc: Long = 0
	internal var done = false
	var modifiedTime: Long = 0

	fun setOS(os: Int) {
		if (0 <= os && os <= 13 || os == 255)
			this.os = os
		else
			throw IllegalArgumentException("os: " + os)
	}

	fun getOS(): Int {
		return os
	}

	fun setName(name: String) {
		this.name = name.toByteArray(ISO_8859_1)

	}

	fun getName(): String {
		if (name == null) return ""
		return name!!.toString(ISO_8859_1)

	}

	fun setComment(comment: String) {
		this.comment = comment.toByteArray(ISO_8859_1)

	}

	fun getComment(): String {
		if (comment == null) return ""
		return comment!!.toString(ISO_8859_1)
	}

	internal fun put(d: Deflate) {
		var flag = 0
		if (text) {
			flag = flag or 1     // FTEXT
		}
		if (fhcrc) {
			flag = flag or 2     // FHCRC
		}
		if (extra != null) {
			flag = flag or 4     // FEXTRA
		}
		if (name != null) {
			flag = flag or 8    // FNAME
		}
		if (comment != null) {
			flag = flag or 16   // FCOMMENT
		}
		var xfl = 0
		if (d.level == JZlib.Z_BEST_SPEED) {
			xfl = xfl or 4
		} else if (d.level == JZlib.Z_BEST_COMPRESSION) {
			xfl = xfl or 2
		}

		d.put_short(0x8b1f.toShort().toInt())  // ID1 ID2
		d.put_byte(8.toByte())         // CM(Compression Method)
		d.put_byte(flag.toByte())
		d.put_byte(modifiedTime.toByte())
		d.put_byte((modifiedTime shr 8).toByte())
		d.put_byte((modifiedTime shr 16).toByte())
		d.put_byte((modifiedTime shr 24).toByte())
		d.put_byte(xfl.toByte())
		d.put_byte(os.toByte())

		if (extra != null) {
			d.put_byte(extra!!.size.toByte())
			d.put_byte((extra!!.size shr 8).toByte())
			d.put_byte(extra, 0, extra!!.size)
		}

		if (name != null) {
			d.put_byte(name, 0, name!!.size)
			d.put_byte(0.toByte())
		}

		if (comment != null) {
			d.put_byte(comment, 0, comment!!.size)
			d.put_byte(0.toByte())
		}
	}

	public fun clone(): Any {
		val gheader = GZIPHeader() as GZIPHeader
		var tmp: ByteArray
		if (gheader.extra != null) {
			tmp = ByteArray(gheader.extra!!.size)
			System.arraycopy(gheader.extra!!, 0, tmp, 0, tmp.size)
			gheader.extra = tmp
		}

		if (gheader.name != null) {
			tmp = ByteArray(gheader.name!!.size)
			System.arraycopy(gheader.name!!, 0, tmp, 0, tmp.size)
			gheader.name = tmp
		}

		if (gheader.comment != null) {
			tmp = ByteArray(gheader.comment!!.size)
			System.arraycopy(gheader.comment!!, 0, tmp, 0, tmp.size)
			gheader.comment = tmp
		}

		return gheader
	}

	companion object {

		val OS_MSDOS = 0x00.toByte()
		val OS_AMIGA = 0x01.toByte()
		val OS_VMS = 0x02.toByte()
		val OS_UNIX = 0x03.toByte()
		val OS_ATARI = 0x05.toByte()
		val OS_OS2 = 0x06.toByte()
		val OS_MACOS = 0x07.toByte()
		val OS_TOPS20 = 0x0a.toByte()
		val OS_WIN32 = 0x0b.toByte()
		val OS_VMCMS = 0x04.toByte()
		val OS_ZSYSTEM = 0x08.toByte()
		val OS_CPM = 0x09.toByte()
		val OS_QDOS = 0x0c.toByte()
		val OS_RISCOS = 0x0d.toByte()
		val OS_UNKNOWN = 0xff.toByte()
	}
}
