package com.soywiz.korio.vfs.js

import com.jtransc.js.*
import com.soywiz.korio.async.suspendCancellableCoroutine

object BrowserJsUtils {
	suspend fun readRangeBytes(url: String, start: Double, end: Double): ByteArray = suspendCancellableCoroutine { c ->
		val xhr = jsNew("XMLHttpRequest")
		xhr.call("open", "GET", url, true)
		xhr["responseType"] = "arraybuffer"

		xhr["onload"] = jsFunctionRaw1 { e ->
			val u8array = jsNew("Uint8Array", xhr["response"])
			val out = ByteArray(u8array["length"].toInt())
			(out.asJsDynamic()).call("setArraySlice", 0, u8array)
			c.resume(out)
		}

		xhr["onerror"] = jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error ${xhr["status"].toJavaString()} opening $url"))
		}

		if (start >= 0 && end >= 0) {
			xhr.call("setRequestHeader", "Range", "bytes=$start-$end")
		}

		c.onCancel {
			xhr.call("abort")
		}

		xhr.call("send")
	}

	suspend fun stat(url: String): JsStat = suspendCancellableCoroutine { c ->
		val xhr = jsNew("XMLHttpRequest")
		xhr.call("open", "HEAD", url, true)
		xhr["onreadystatechange"] = jsFunctionRaw0 {
			if (xhr["readyState"].eq(xhr["DONE"])) {
				val len = global.call("parseFloat", xhr.call("getResponseHeader", "Content-Length"))
				val out = JsStat(len.toDouble())
				c.resume(out)
			}
		}
		xhr["onerror"] = jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error ${xhr["status"].toJavaString()} opening $url"))
		}

		c.onCancel {
			xhr.call("abort")
		}

		xhr.call("send")
	}

	fun getBaseUrl(): String {
		var baseHref = document["location"]["href"].call("replace", jsRegExp("/[^\\/]*$"), "")
		val bases = document.call("getElementsByTagName", "base")
		if (bases["length"].toInt() > 0) baseHref = bases[0]["href"]
		return baseHref.toJavaString()
	}
}

private fun jsRegExp(regex: String): JsDynamic? = global["RegExp"].new(regex)
