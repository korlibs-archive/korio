@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs.js

import com.jtransc.js.*
import com.soywiz.korio.async.suspendCancellableCoroutine
import com.soywiz.korio.coroutine.korioSuspendCoroutine

object NodeJsUtils {
	suspend fun readRangeBytes(path: String, start: Double, end: Double): ByteArray = suspendCancellableCoroutine { c ->
		val http = jsRequire("http")
		val url = jsRequire("url")
		val info = url.methods["parse"](path)
		val headers = jsObject()

		if (start >= 0 && end >= 0) headers["Range"] = "bytes=$start-$end"

		val r = http.methods["get"](jsObject(
			"host" to info["hostname"],
			"port" to info["port"],
			"path" to info["path"],
			"agent" to false,
			"encoding" to null,
			"headers" to headers
		), jsFunctionRaw1 { res ->
			val body = jsArray()
			res.methods["on"]("data", jsFunctionRaw1 { d -> body.methods["push"](d) })
			res.methods["on"]("end", jsFunctionRaw0 {
				val r = global["Buffer"].methods["concat"](body)
				val u8array = jsNew("Int8Array", r)
				val out = ByteArray(r["length"].toInt())
				out.asJsDynamic().methods["setArraySlice"](0, u8array)
				c.resume(out)
			})
		}).methods["on"]("error", jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error: ${e.toJavaString()}"))
		})

		c.onCancel {
			r.methods["abort"]()
		}
	}

	suspend fun httpStat(path: String): JsStat = suspendCancellableCoroutine { c ->
		val http = jsRequire("http")
		val url = jsRequire("url")
		val info = url.methods["parse"](path)

		val r = http.methods["get"](jsObject(
			"method" to "HEAD",
			"host" to info["hostname"],
			"port" to info["port"],
			"path" to info["path"]
		), jsFunctionRaw1 { res ->
			val len = global.methods["parseFloat"](res["headers"]["content-length"])
			val out = JsStat(len.toDouble())
			c.resume(out)
		}).methods["on"]("error", jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error: ${e.toJavaString()}"))
		})

		c.onCancel {
			r.methods["abort"]()
		}

	}

	suspend fun open(path: String, mode: String): JsDynamic = korioSuspendCoroutine { c ->
		val fs = jsRequire("fs")
		fs.methods["open"](path, mode, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				c.resume(fd!!)
			}
		})
	}

	suspend fun read(fd: JsDynamic?, position: Double, len: Double): ByteArray = korioSuspendCoroutine { c ->
		val fs = jsRequire("fs")
		val buffer = jsNew("Buffer", len)
		fs.methods["read"](fd, buffer, 0, len, position, jsFunctionRaw3 { err, bytesRead, buffer ->
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} opening ${fd.toJavaString()}"))
			} else {
				val u8array = jsNew("Int8Array", buffer, 0, bytesRead)
				val out = ByteArray(bytesRead.toInt())
				out.asJsDynamic().methods["setArraySlice"](0, u8array)
				c.resume(out)
			}
		})
	}

	suspend fun close(fd: Any): Unit = korioSuspendCoroutine { c ->
		val fs = jsRequire("fs")
		fs.methods["close"](fd, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} closing file"))
			} else {
				c.resume(Unit)
			}
		})
	}

	fun getCWD(): String = global["process"].methods["cwd"]().toJavaString()

	suspend fun fstat(path: String): JsStat = korioSuspendCoroutine { c ->
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
		val fs = jsRequire("fs")
		//fs.methods["exists"](path, jsFunctionRaw1 { jsexists ->
		//	val exists = jsexists.toBool()
		//	if (exists) {
		fs.methods["stat"](path, jsFunctionRaw2 { err, stat ->
			//console.methods["log"](stat)
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				val out = JsStat(stat["size"].toDouble())
				out.isDirectory = stat.methods["isDirectory"]().toBool()
				c.resume(out)
			}
		})
		//	} else {
		//		c.resumeWithException(RuntimeException("File '$path' doesn't exists"))
		//	}
		//})
	}
}