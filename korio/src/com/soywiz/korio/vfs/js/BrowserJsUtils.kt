package com.soywiz.korio.vfs.js

import com.jtransc.js.*
import kotlin.coroutines.suspendCoroutine

object BrowserJsUtils {
	suspend fun readRangeBytes(url: String, start: Double, end: Double): ByteArray = suspendCoroutine { c ->
		val xhr = jsNew("XMLHttpRequest")
		xhr.methods["open"]("GET", url, true);
		xhr["responseType"] = "arraybuffer"

		xhr["onload"] = jsFunctionRaw1 { e ->
			val u8array = jsNew("Uint8Array", xhr["response"])
			val out = ByteArray(u8array["length"].toInt());
			(out.asJsDynamic()).methods["setArraySlice"](0, u8array)
			c.resume(out)
		};

		xhr["onerror"] = jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error ${xhr["status"].toJavaString()} opening $url"))
		};

		if (start >= 0 && end >= 0) xhr.methods["setRequestHeader"]("Range", "bytes=$start-$end");
		xhr.methods["send"]();
	}

	suspend fun stat(url: String): JsStat = suspendCoroutine { c ->
		val xhr = jsNew("XMLHttpRequest")
		xhr.methods["open"]("HEAD", url, true);
		xhr["onreadystatechange"] = jsFunctionRaw0 {
			if (xhr["readyState"].eq(xhr["DONE"])) {
				val len = global.methods["parseFloat"](xhr.methods["getResponseHeader"]("Content-Length"))
				val out = JsStat(len.toDouble())
				c.resume(out)
			}
		}
		xhr["onerror"] = jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error ${xhr["status"].toJavaString()} opening $url"))
		};
		xhr.methods["send"]();
	}

	fun getBaseUrl(): String {
		var baseHref = document["location"]["href"].methods["replace"](jsRegExp("/[^\\/]*$"), "")
		val bases = document.methods["getElementsByTagName"]("base");
		if (bases["length"].toInt() > 0) baseHref = bases[0]["href"];
		return baseHref.toJavaString()
	}
}

private fun jsRegExp(regex: String): JsDynamic? = global["RegExp"].new(regex)
