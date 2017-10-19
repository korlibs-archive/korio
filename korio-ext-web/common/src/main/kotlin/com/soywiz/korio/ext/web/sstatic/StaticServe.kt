package com.soywiz.korio.ext.web.sstatic

import com.soywiz.korio.crypto.AsyncHash
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpDate
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.stream.copyTo
import com.soywiz.korio.stream.slice
import com.soywiz.korio.util.toHexStringLower
import com.soywiz.korio.util.use
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.mimeType

object StaticServe {
	// https://tools.ietf.org/html/rfc7233#section-3.1
	fun parseRange(str: String?, size: Long): Iterable<LongRange> {
		if (str == null) return listOf(0L until size)
		val parts = str.split('=', limit = 2).map { it.trim() }
		if (parts[0] != "bytes") invalidOp("Just supported bytes")
		val ranges = parts[1].split(',')
		val out = ArrayList<LongRange>()
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
		return "" + AsyncHash.SHA1.hashSync(name).toHexStringLower() + "-" + lastModified + "-" + size
		// @TODO: Calling asynchronous hash fails tests! Because it doesn't wait.
		//return "" + AsyncHash.SHA1.hash(name).toHexStringLower() + "-" + lastModified + "-" + size
	}

	suspend fun serveStatic(res: HttpServer.Request, file: VfsFile) {
		//println("[a]")
		val fileStat = file.stat()
		if (!fileStat.exists) throw com.soywiz.korio.FileNotFoundException("$file not found")
		//println("[b]")

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
		//println("[c]")
		res.replaceHeader("Content-Type", file.mimeType().mime)
		res.replaceHeader("Last-Modified", HttpDate.format(fileStat.modifiedTime))
		res.replaceHeader("ETag", generateETag(file.absolutePath, fileStat.modifiedTime, fileStat.size))
		//println("[d]")

		val contentLength = when {
			head -> 0L
			hasRange -> (range.endInclusive - range.start) + 1
			else -> fileSize
		}

		if (hasRange) {
			res.replaceHeader("Content-Range", "bytes ${range.start}-${range.endInclusive}/$fileSize")
		}

		//println("[e]")

		res.replaceHeader("Content-Length", "$contentLength")

		//println("----------------")
		if (!head) {
			if (hasRange) {
				file.openRead().use { slice(range).copyTo(res) }
			} else {
				file.copyTo(res)
			}
		}

		//println("[f]")

		res.close()

		//println("[g]")
	}
}

suspend fun HttpServer.Request.serveStatic(file: VfsFile) = StaticServe.serveStatic(this, file)

