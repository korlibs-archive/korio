/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2000,2001,2002,2003 ymnk, JCraft,Inc. All rights reserved.

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
import com.soywiz.korio.lang.and

internal class InfCodes(private val z: ZStream, private val s: InfBlocks) {

	var mode: Int = 0      // current inflate_codes mode

	// mode dependent information
	var len: Int = 0

	var tree: IntArray? = null // pointer into tree
	var tree_index = 0
	var need: Int = 0   // bits needed

	var lit: Int = 0

	// if EXT or COPY, where and how much
	var get: Int = 0              // bits to get for extra
	var dist: Int = 0             // distance back to copy from

	var lbits: Byte = 0           // ltree bits decoded per branch
	var dbits: Byte = 0           // dtree bits decoder per branch
	lateinit var ltree: IntArray          // literal/length/eob tree
	var ltree_index: Int = 0      // literal/length/eob tree
	lateinit var dtree: IntArray          // distance tree
	var dtree_index: Int = 0      // distance tree

	fun init(bl: Int, bd: Int,
			 tl: IntArray, tl_index: Int,
			 td: IntArray, td_index: Int) {
		mode = START
		lbits = bl.toByte()
		dbits = bd.toByte()
		ltree = tl
		ltree_index = tl_index
		dtree = td
		dtree_index = td_index
		tree = null
	}

	fun proc(r: Int): Int {
		var r = r
		var j: Int              // temporary storage
		val t: IntArray            // temporary pointer
		var tindex: Int = 0         // temporary pointer
		var e: Int = 0              // extra bits or operation
		var b = 0            // bit buffer
		var k = 0            // bits in bit buffer
		var p = 0            // input data pointer
		var n: Int              // bytes available there
		var q: Int              // output window write pointer
		var m: Int              // bytes to end of window or read pointer
		var f: Int              // pointer to copy strings from

		// copy input/output information to locals (UPDATE macro restores)
		p = z.next_in_index
		n = z.avail_in
		b = s.bitb
		k = s.bitk
		q = s.write
		m = if (q < s.read) s.read - q - 1 else s.end - q

		// process input and output based on current state
		while (true) {
			lwhen@do {
				when (mode) {
				// waiting for "i:"=input, "o:"=output, "x:"=nothing
					START -> {        // x: set up for LEN

						if (m >= 258 && n >= 10) {

							s.bitb = b
							s.bitk = k
							z.avail_in = n
							z.total_in += p - z.next_in_index
							z.next_in_index = p
							s.write = q
							r = inflate_fast(lbits.toInt(), dbits.toInt(),
								ltree, ltree_index,
								dtree, dtree_index,
								s, z)

							p = z.next_in_index
							n = z.avail_in
							b = s.bitb
							k = s.bitk
							q = s.write
							m = if (q < s.read) s.read - q - 1 else s.end - q

							if (r != Z_OK) {
								mode = if (r == Z_STREAM_END) WASH else BADCODE
								break@lwhen
							}
						}
						need = lbits.toInt()
						tree = ltree
						tree_index = ltree_index

						mode = LEN
						j = need

						while (k < j) {
							if (n != 0)
								r = Z_OK
							else {

								s.bitb = b
								s.bitk = k
								z.avail_in = n
								z.total_in += p - z.next_in_index
								z.next_in_index = p
								s.write = q
								return s.inflate_flush(r)
							}
							n--
							b = b or (z.next_in!![p++] and 0xff shl k)
							k += 8
						}

						tindex = (tree_index + (b and inflate_mask[j])) * 3

						b = b ushr tree!![tindex + 1]
						k -= tree!![tindex + 1]

						e = tree!![tindex]

						if (e == 0) {               // literal
							lit = tree!![tindex + 2]
							mode = LIT
							break@lwhen
						}
						if (e and 16 != 0) {          // length
							get = e and 15
							len = tree!![tindex + 2]
							mode = LENEXT
							break@lwhen
						}
						if (e and 64 == 0) {        // next table
							need = e
							tree_index = tindex / 3 + tree!![tindex + 2]
							break@lwhen
						}
						if (e and 32 != 0) {               // end of block
							mode = WASH
							break@lwhen
						}
						mode = BADCODE        // invalid code
						z.msg = "invalid literal/length code"
						r = Z_DATA_ERROR

						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
					LEN -> {         // i: get length/literal/eob next

						j = need
						while (k < j) {
							if (n != 0)
								r = Z_OK
							else {
								s.bitb = b
								s.bitk = k
								z.avail_in = n
								z.total_in += p - z.next_in_index
								z.next_in_index = p
								s.write = q
								return s.inflate_flush(r)
							}
							n--
							b = b or (z.next_in!![p++] and 0xff shl k)
							k += 8
						}
						tindex = (tree_index + (b and inflate_mask[j])) * 3
						b = b ushr tree!![tindex + 1]
						k -= tree!![tindex + 1]
						e = tree!![tindex]
						if (e == 0) {
							lit = tree!![tindex + 2]
							mode = LIT
							break@lwhen
						}
						if (e and 16 != 0) {
							get = e and 15
							len = tree!![tindex + 2]
							mode = LENEXT
							break@lwhen
						}
						if (e and 64 == 0) {
							need = e
							tree_index = tindex / 3 + tree!![tindex + 2]
							break@lwhen
						}
						if (e and 32 != 0) {
							mode = WASH
							break@lwhen
						}
						mode = BADCODE
						z.msg = "invalid literal/length code"
						r = Z_DATA_ERROR
						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
					LENEXT -> {       // i: getting length extra (have base)

						j = get

						while (k < j) {
							if (n != 0)
								r = Z_OK
							else {

								s.bitb = b
								s.bitk = k
								z.avail_in = n
								z.total_in += p - z.next_in_index
								z.next_in_index = p
								s.write = q
								return s.inflate_flush(r)
							}
							n--
							b = b or (z.next_in!![p++] and 0xff shl k)
							k += 8
						}

						len += b and inflate_mask[j]

						b = b shr j
						k -= j

						need = dbits.toInt()
						tree = dtree
						tree_index = dtree_index
						mode = DIST
						j = need

						while (k < j) {
							if (n != 0)
								r = Z_OK
							else {

								s.bitb = b
								s.bitk = k
								z.avail_in = n
								z.total_in += p - z.next_in_index
								z.next_in_index = p
								s.write = q
								return s.inflate_flush(r)
							}
							n--
							b = b or (z.next_in!![p++] and 0xff shl k)
							k += 8
						}

						tindex = (tree_index + (b and inflate_mask[j])) * 3

						b = b shr tree!![tindex + 1]
						k -= tree!![tindex + 1]

						e = tree!![tindex]
						if (e and 16 != 0) {               // distance
							get = e and 15
							dist = tree!![tindex + 2]
							mode = DISTEXT
							break@lwhen
						}
						if (e and 64 == 0) {        // next table
							need = e
							tree_index = tindex / 3 + tree!![tindex + 2]
							break@lwhen
						}
						mode = BADCODE        // invalid code
						z.msg = "invalid distance code"
						r = Z_DATA_ERROR

						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
					DIST -> {         // i: get distance next

						j = need
						while (k < j) {
							if (n != 0)
								r = Z_OK
							else {
								s.bitb = b
								s.bitk = k
								z.avail_in = n
								z.total_in += p - z.next_in_index
								z.next_in_index = p
								s.write = q
								return s.inflate_flush(r)
							}
							n--
							b = b or (z.next_in!![p++] and 0xff shl k)
							k += 8
						}
						tindex = (tree_index + (b and inflate_mask[j])) * 3
						b = b shr tree!![tindex + 1]
						k -= tree!![tindex + 1]
						e = tree!![tindex]
						if (e and 16 != 0) {
							get = e and 15
							dist = tree!![tindex + 2]
							mode = DISTEXT
							break@lwhen
						}
						if (e and 64 == 0) {
							need = e
							tree_index = tindex / 3 + tree!![tindex + 2]
							break@lwhen
						}
						mode = BADCODE
						z.msg = "invalid distance code"
						r = Z_DATA_ERROR
						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
					DISTEXT -> {      // i: getting distance extra

						j = get

						while (k < j) {
							if (n != 0)
								r = Z_OK
							else {

								s.bitb = b
								s.bitk = k
								z.avail_in = n
								z.total_in += p - z.next_in_index
								z.next_in_index = p
								s.write = q
								return s.inflate_flush(r)
							}
							n--
							b = b or (z.next_in!![p++] and 0xff shl k)
							k += 8
						}

						dist += b and inflate_mask[j]

						b = b shr j
						k -= j

						mode = COPY
						f = q - dist
						while (f < 0) {     // modulo window size-"while" instead
							f += s.end     // of "if" handles invalid distances
						}
						while (len != 0) {

							if (m == 0) {
								if (q == s.end && s.read != 0) {
									q = 0
									m = if (q < s.read) s.read - q - 1 else s.end - q
								}
								if (m == 0) {
									s.write = q
									r = s.inflate_flush(r)
									q = s.write
									m = if (q < s.read) s.read - q - 1 else s.end - q

									if (q == s.end && s.read != 0) {
										q = 0
										m = if (q < s.read) s.read - q - 1 else s.end - q
									}

									if (m == 0) {
										s.bitb = b
										s.bitk = k
										z.avail_in = n
										z.total_in += p - z.next_in_index
										z.next_in_index = p
										s.write = q
										return s.inflate_flush(r)
									}
								}
							}

							s!!.window!![q++] = s!!.window!![f++]
							m--

							if (f == s.end)
								f = 0
							len--
						}
						mode = START
					}
					COPY -> {         // o: copying bytes in window, waiting for space

						f = q - dist
						while (f < 0) {
							f += s.end
						}
						while (len != 0) {
							if (m == 0) {
								if (q == s.end && s.read != 0) {
									q = 0
									m = if (q < s.read) s.read - q - 1 else s.end - q
								}
								if (m == 0) {
									s.write = q
									r = s.inflate_flush(r)
									q = s.write
									m = if (q < s.read) s.read - q - 1 else s.end - q
									if (q == s.end && s.read != 0) {
										q = 0
										m = if (q < s.read) s.read - q - 1 else s.end - q
									}
									if (m == 0) {
										s.bitb = b
										s.bitk = k
										z.avail_in = n
										z.total_in += p - z.next_in_index
										z.next_in_index = p
										s.write = q
										return s.inflate_flush(r)
									}
								}
							}
							s!!.window!![q++] = s!!.window!![f++]
							m--
							if (f == s.end)
								f = 0
							len--
						}
						mode = START
					}
					LIT -> {          // o: got literal, waiting for output space

						if (m == 0) {
							if (q == s.end && s.read != 0) {
								q = 0
								m = if (q < s.read) s.read - q - 1 else s.end - q
							}
							if (m == 0) {
								s.write = q
								r = s.inflate_flush(r)
								q = s.write
								m = if (q < s.read) s.read - q - 1 else s.end - q

								if (q == s.end && s.read != 0) {
									q = 0
									m = if (q < s.read) s.read - q - 1 else s.end - q
								}
								if (m == 0) {
									s.bitb = b
									s.bitk = k
									z.avail_in = n
									z.total_in += p - z.next_in_index
									z.next_in_index = p
									s.write = q
									return s.inflate_flush(r)
								}
							}
						}
						r = Z_OK

						s.window!![q++] = lit.toByte()
						m--

						mode = START
					}
					WASH -> {          // o: got eob, possibly more output

						if (k > 7) {        // return unused byte, if any
							k -= 8
							n++
							p--             // can always return one
						}

						s.write = q
						r = s.inflate_flush(r)
						q = s.write
						m = if (q < s.read) s.read - q - 1 else s.end - q

						if (s.read != s.write) {
							s.bitb = b
							s.bitk = k
							z.avail_in = n
							z.total_in += p - z.next_in_index
							z.next_in_index = p
							s.write = q
							return s.inflate_flush(r)
						}
						mode = END
						r = Z_STREAM_END
						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
					END -> {
						r = Z_STREAM_END
						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
					BADCODE -> {      // x: got error
						r = Z_DATA_ERROR

						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
					else -> {
						r = Z_STREAM_ERROR

						s.bitb = b
						s.bitk = k
						z.avail_in = n
						z.total_in += p - z.next_in_index
						z.next_in_index = p
						s.write = q
						return s.inflate_flush(r)
					}
				}
			} while (false)
		}
	}

	fun free(z: ZStream) {
		//  ZFREE(z, c);
	}

	// Called with number of bytes left to write in window at least 258
	// (the maximum string length) and number of input bytes available
	// at least ten.  The ten bytes are six bytes for the longest length/
	// distance pair plus four bytes for overloading the bit buffer.

	fun inflate_fast(bl: Int, bd: Int,
					 tl: IntArray, tl_index: Int,
					 td: IntArray, td_index: Int,
					 s: InfBlocks, z: ZStream): Int {
		var t: Int                // temporary pointer
		var tp: IntArray             // temporary pointer
		var tp_index: Int         // temporary pointer
		var e: Int                // extra bits or operation
		var b: Int                // bit buffer
		var k: Int                // bits in bit buffer
		var p: Int                // input data pointer
		var n: Int                // bytes available there
		var q: Int                // output window write pointer
		var m: Int                // bytes to end of window or read pointer
		val ml: Int               // mask for literal/length tree
		val md: Int               // mask for distance tree
		var c: Int                // bytes to copy
		var d: Int                // distance back to copy from
		var r: Int                // copy source pointer

		var tp_index_t_3: Int     // (tp_index+t)*3

		// load input, output, bit values
		p = z.next_in_index
		n = z.avail_in
		b = s.bitb
		k = s.bitk
		q = s.write
		m = if (q < s.read) s.read - q - 1 else s.end - q

		// initialize masks
		ml = inflate_mask[bl]
		md = inflate_mask[bd]

		// do until not enough input or output space for fast loop
		do {                          // assume called with m >= 258 && n >= 10
			// get literal/length code
			while (k < 20) {              // max bits for literal/length code
				n--
				b = b or (z.next_in!![p++] and 0xff shl k)
				k += 8
			}

			t = b and ml
			tp = tl
			tp_index = tl_index
			tp_index_t_3 = (tp_index + t) * 3
			e = tp[tp_index_t_3]
			if (e == 0) {
				b = b shr tp[tp_index_t_3 + 1]
				k -= tp[tp_index_t_3 + 1]

				s.window!![q++] = tp[tp_index_t_3 + 2].toByte()
				m--
				continue
			}
			do {

				b = b shr tp[tp_index_t_3 + 1]
				k -= tp[tp_index_t_3 + 1]

				if (e and 16 != 0) {
					e = e and 15
					c = tp[tp_index_t_3 + 2] + (b.toInt() and inflate_mask[e])

					b = b shr e
					k -= e

					// decode distance base of block to copy
					while (k < 15) {           // max bits for distance code
						n--
						b = b or (z.next_in!![p++] and 0xff shl k)
						k += 8
					}

					t = b and md
					tp = td
					tp_index = td_index
					tp_index_t_3 = (tp_index + t) * 3
					e = tp[tp_index_t_3]

					do {

						b = b shr tp[tp_index_t_3 + 1]
						k -= tp[tp_index_t_3 + 1]

						if (e and 16 != 0) {
							// get extra bits to add to distance base
							e = e and 15
							while (k < e) {         // get extra bits (up to 13)
								n--
								b = b or (z.next_in!![p++] and 0xff shl k)
								k += 8
							}

							d = tp[tp_index_t_3 + 2] + (b and inflate_mask[e])

							b = b shr e
							k -= e

							// do the copy
							m -= c
							if (q >= d) {                // offset before dest
								//  just copy
								r = q - d
								if (q - r > 0 && 2 > q - r) {
									s.window!![q++] = s.window!![r++] // minimum count is three,
									s.window!![q++] = s.window!![r++] // so unroll loop a little
									c -= 2
								} else {
									System.arraycopy(s.window!!, r, s.window!!, q, 2)
									q += 2
									r += 2
									c -= 2
								}
							} else {                  // else offset after destination
								r = q - d
								do {
									r += s.end          // force pointer in window
								} while (r < 0)         // covers invalid distances
								e = s.end - r
								if (c > e) {             // if source crosses,
									c -= e              // wrapped copy
									if (q - r > 0 && e > q - r) {
										do {
											s.window!![q++] = s.window!![r++]
										} while (--e != 0)
									} else {
										System.arraycopy(s.window!!, r, s.window!!, q, e)
										q += e
										r += e
										e = 0
									}
									r = 0                  // copy rest from start of window
								}

							}

							// copy all or what's left
							if (q - r > 0 && c > q - r) {
								do {
									s.window!![q++] = s.window!![r++]
								} while (--c != 0)
							} else {
								System.arraycopy(s.window!!, r, s.window!!, q, c)
								q += c
								r += c
								c = 0
							}
							break
						} else if (e and 64 == 0) {
							t += tp[tp_index_t_3 + 2]
							t += b and inflate_mask[e]
							tp_index_t_3 = (tp_index + t) * 3
							e = tp[tp_index_t_3]
						} else {
							z.msg = "invalid distance code"

							c = z.avail_in - n
							c = if (k shr 3 < c) k shr 3 else c
							n += c
							p -= c
							k -= c shl 3

							s.bitb = b
							s.bitk = k
							z.avail_in = n
							z.total_in += p - z.next_in_index
							z.next_in_index = p
							s.write = q

							return Z_DATA_ERROR
						}
					} while (true)
					break
				}

				if (e and 64 == 0) {
					t += tp[tp_index_t_3 + 2]
					t += b and inflate_mask[e]
					tp_index_t_3 = (tp_index + t) * 3
					e = tp[tp_index_t_3]
					if (e == 0) {

						b = b shr tp[tp_index_t_3 + 1]
						k -= tp[tp_index_t_3 + 1]

						s.window!![q++] = tp[tp_index_t_3 + 2].toByte()
						m--
						break
					}
				} else if (e and 32 != 0) {

					c = z.avail_in - n
					c = if (k shr 3 < c) k shr 3 else c
					n += c
					p -= c
					k -= c shl 3

					s.bitb = b
					s.bitk = k
					z.avail_in = n
					z.total_in += p - z.next_in_index
					z.next_in_index = p
					s.write = q

					return Z_STREAM_END
				} else {
					z.msg = "invalid literal/length code"

					c = z.avail_in - n
					c = if (k shr 3 < c) k shr 3 else c
					n += c
					p -= c
					k -= c shl 3

					s.bitb = b
					s.bitk = k
					z.avail_in = n
					z.total_in += p - z.next_in_index
					z.next_in_index = p
					s.write = q

					return Z_DATA_ERROR
				}
			} while (true)
		} while (m >= 258 && n >= 10)

		// not enough input or output--restore pointers and return
		c = z.avail_in - n
		c = if (k shr 3 < c) k shr 3 else c
		n += c
		p -= c
		k -= c shl 3

		s.bitb = b
		s.bitk = k
		z.avail_in = n
		z.total_in += p - z.next_in_index
		z.next_in_index = p
		s.write = q

		return Z_OK
	}

	companion object {

		private val inflate_mask = intArrayOf(0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff)

		private val Z_OK = 0
		private val Z_STREAM_END = 1
		private val Z_NEED_DICT = 2
		private val Z_ERRNO = -1
		private val Z_STREAM_ERROR = -2
		private val Z_DATA_ERROR = -3
		private val Z_MEM_ERROR = -4
		private val Z_BUF_ERROR = -5
		private val Z_VERSION_ERROR = -6

		// waiting for "i:"=input,
		//             "o:"=output,
		//             "x:"=nothing
		private val START = 0  // x: set up for LEN
		private val LEN = 1    // i: get length/literal/eob next
		private val LENEXT = 2 // i: getting length extra (have base)
		private val DIST = 3   // i: get distance next
		private val DISTEXT = 4// i: getting distance extra
		private val COPY = 5   // o: copying bytes in window, waiting for space
		private val LIT = 6    // o: got literal, waiting for output space
		private val WASH = 7   // o: got eob, possibly still output waiting
		private val END = 8    // x: got eob and all data flushed
		private val BADCODE = 9// x: got error
	}
}
