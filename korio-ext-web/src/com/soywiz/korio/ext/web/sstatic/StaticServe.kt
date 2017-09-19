package com.soywiz.korio.ext.web.sstatic

import com.soywiz.korio.crypto.AsyncHash
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpDate
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.stream.copyTo
import com.soywiz.korio.stream.slice
import com.soywiz.korio.util.toHexString
import com.soywiz.korio.util.toHexStringLower
import com.soywiz.korio.util.use
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.mimeType
import java.io.FileNotFoundException
import java.util.*


object StaticServe {
	// https://tools.ietf.org/html/rfc7233#section-3.1
	/*
	o  The final 500 bytes (byte offsets 9500-9999, inclusive):

        bytes=-500

   Or:

        bytes=9500-

   o  The first and last bytes only (bytes 0 and 9999):

        bytes=0-0,-1

   o  Other valid (but not canonical) specifications of the second 500
      bytes (byte offsets 500-999, inclusive):

        bytes=500-600,601-999
        bytes=500-700,601-999

 The following are examples of Content-Range values in which the
   selected representation contains a total of 1234 bytes:

   o  The first 500 bytes:

        Content-Range: bytes 0-499/1234

   o  The second 500 bytes:

        Content-Range: bytes 500-999/1234

   o  All except for the first 500 bytes:

        Content-Range: bytes 500-1233/1234

   o  The last 500 bytes:

        Content-Range: bytes 734-1233/1234

	 */
	fun parseRange(str: String?, size: Long): Iterable<LongRange> {
		if (str == null) return listOf(0L until size)
		val parts = str.split('=', limit = 2).map { it.trim() }
		if (parts[0] != "bytes") invalidOp("Just supported bytes")
		val ranges = parts[1].split(',')
		val out = arrayListOf<LongRange>()
		for (range in ranges) {
			if (range.startsWith("-")) {
				out += (size + range.toLong()) until size
			} else {
				val parts2 = range.split('-', limit = 2)
				val start = parts2[0].toLong()
				val end = parts2[1].toLongOrNull() ?: (size - 1)
				out += start..end
			}
		}
		return out
	}

	fun combineRanges(ranges: Iterable<LongRange>): LongRange {
		// @TODO:
		return ranges.firstOrNull() ?: 0L..-1L
	}

	suspend fun generateETag(name: String, lastModified: Long, size: Long): String {
		//return Map
		return "" + AsyncHash.SHA1.hash(name).toHexStringLower() + "-" + lastModified + "-" + size
	}

	suspend fun serveStatic(res: HttpServer.Request, file: VfsFile) {
		val fileStat = file.stat()
		if (!fileStat.exists) throw FileNotFoundException()

		val fileSize = fileStat.size
		val rangeStr = res.getHeader("Range")
		val ranges = parseRange(rangeStr, fileSize)
		val range = combineRanges(ranges)
		val hasRange = rangeStr != null
		val head = res.method == Http.Methods.HEAD
		res.setStatus(if (hasRange) 206 else 200)
		if (fileStat.exists) {
			res.replaceHeader("Accept-Ranges", "bytes")
		}
		res.replaceHeader("Content-Type", file.mimeType().mime)
		res.replaceHeader("Last-Modified", HttpDate.format(Date(fileStat.modifiedTime)))
		res.replaceHeader("ETag", generateETag(file.toString(), fileStat.modifiedTime, fileStat.size))

		val contentLength = when {
			head -> 0L
			hasRange -> (range.endInclusive - range.start) + 1
			else -> fileSize
		}

		if (hasRange) {
			res.replaceHeader("Content-Range", "bytes ${range.start}-${range.endInclusive}/$fileSize")
		}

		res.replaceHeader("Content-Length", "$contentLength")

		if (!head) {
			if (hasRange) {
				file.openRead().use { slice(range).copyTo(res) }
			} else {
				file.copyTo(res)
			}
		}
		res.close()
	}
}

suspend fun HttpServer.Request.serveStatic(file: VfsFile) = StaticServe.serveStatic(this, file)

