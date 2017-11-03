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

class GZIPOutputStream constructor(
	out: OutputStream,
	deflater: Deflater,
	size: Int,
	close_out: Boolean
) : DeflaterOutputStream(out, deflater, size, close_out) {

	/*FINISH_STATE*/ val crc: Long
		get() {
			if (deflater.dstate!!.status != 666) throw GZIPException("checksum is not calculated yet.")
			return deflater.dstate!!.gzipHeader.crc
		}

	constructor(out: OutputStream, size: Int = DeflaterOutputStream.DEFAULT_BUFSIZE, close_out: Boolean = true)
		: this(out, Deflater(JZlib.Z_DEFAULT_COMPRESSION, 15 + 16), size, close_out) {
		mydeflater = true
	}


	private fun check() {
		if (deflater.dstate!!.status != 42 /*INIT_STATUS*/) throw GZIPException("header is already written.")
	}

	fun setModifiedTime(mtime: Long) {
		check()
		deflater.dstate!!.gzipHeader.modifiedTime = mtime
	}

	fun setOS(os: Int) {
		check()
		deflater.dstate!!.gzipHeader.setOS(os)
	}

	fun setName(name: String) {
		check()
		deflater.dstate!!.gzipHeader.setName(name)
	}

	fun setComment(comment: String) {
		check()
		deflater.dstate!!.gzipHeader.setComment(comment)
	}
}
