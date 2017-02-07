package com.soywiz.korio.net.http

import com.jtransc.js.*
import com.soywiz.korio.async.Promise
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.OS

class HttpClientFactoryJs : HttpFactory() {
	override fun createClient(): HttpClient = if (OS.isNodejs) HttpClientNodeJs() else HttpClientBrowserJs()
}

class HttpClientNodeJs : HttpClient() {
	suspend override fun request(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		println(url)

		val http = jsRequire("http")
		val jsurl = jsRequire("url")
		val info = jsurl.call("parse", url)
		val reqHeaders = jsObject()

		for (header in headers) reqHeaders[header.first] = header.second

		val r = http.call("request", jsObject(
				"method" to method.name,
				"host" to info["hostname"],
				"port" to info["port"],
				"path" to info["path"],
				"agent" to false,
				"encoding" to null,
				"headers" to reqHeaders
		), jsFunctionRaw1 { res ->
			val statusCode = res["statusCode"].toInt()
			val statusMessage = res["statusMessage"].toJavaStringOrNull() ?: ""
			val jsHeadersObj = res["headers"]
			val body = jsArray()
			res.call("on", "data", jsFunctionRaw1 { d -> body.call("push", d) })
			res.call("on", "end", jsFunctionRaw0 {
				val r = global["Buffer"].call("concat", body)
				val u8array = jsNew("Int8Array", r)
				val out = u8array.toByteArray()

				deferred.resolve(Response(
						status = statusCode,
						statusText = statusMessage,
						headers = Http.Headers((jsHeadersObj?.toObjectMap() ?: mapOf()).mapValues { it.value.toJavaStringOrNull() ?: "" }),
						content = out.openAsync()
				))
			})
		}).call("on", "error", jsFunctionRaw1 { e ->
			deferred.reject(RuntimeException("Error: ${e.toJavaString()}"))
		})

		deferred.onCancel {
			r.call("abort")
		}

		if (content != null) {
			r.call("end", content.readAll().toTypedArray())
		} else {
			r.call("end")
		}

	}
}

class HttpClientBrowserJs : HttpClient() {
	suspend override fun request(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		val xhr = jsNew("XMLHttpRequest")
		xhr.call("open", "GET", url, true)
		xhr["responseType"] = "arraybuffer"

		xhr["onload"] = jsFunctionRaw1 { e ->
			val u8array = jsNew("Uint8Array", xhr["response"])
			val out = ByteArray(u8array["length"].toInt())
			(out.asJsDynamic()).call("setArraySlice", 0, u8array)
			deferred.resolve(HttpClient.Response(
					status = xhr["status"].toInt(),
					statusText = xhr["statusText"].toJavaStringOrNull() ?: "",
					headers = Http.Headers(xhr.call("getAllResponseHeaders").toJavaStringOrNull()),
					content = out.openAsync()
			))
		}

		xhr["onerror"] = jsFunctionRaw1 { e ->
			deferred.reject(RuntimeException("Error ${xhr["status"].toJavaString()} opening $url"))
		}

		for (header in headers) xhr.call("setRequestHeader", header.first, header.second)

		deferred.onCancel { xhr.call("abort") }

		if (content != null) {
			xhr.call("send", content.readAll().toJsTypedArray())
		} else {
			xhr.call("send")
		}
	}
}

fun JsDynamic.arrayToList(): List<JsDynamic?> {
	val array = this
	return (0 until array["length"].toInt()).map { array[it] }
}

fun JsDynamic.getObjectKeys(): List<String> {
	val keys = global["Object"].call("keys", this)
	return (0 until keys["length"].toInt()).map { keys[it].toJavaStringOrNull() ?: "" }
}

fun JsDynamic.toObjectMap(): Map<String, JsDynamic?> = (getObjectKeys()).map { key -> key to this[key] }.toMap()


/*
class UrlVfsProviderJs : UrlVfsProvider {
	override fun invoke(): Vfs = object : Vfs() {
		val statCache = AsyncCache()

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			var info: VfsStat? = null

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					val res = JsUtils.readRangeBytes(path, position.toDouble(), (position + len - 1).toDouble())
					System.arraycopy(res, 0, buffer, offset, res.size)
					return res.size
				}

				suspend override fun getLength(): Long {
					if (info == null) info = stat(path)
					return info!!.size
				}
			}.toAsyncStream().buffered()
		}

		//suspend override fun readFully(path: String): ByteArray = JsUtils.readBytes(path)

		suspend override fun stat(path: String): VfsStat = statCache(path) {
			try {
				JsUtils.stat(path).toStat(path, this)
			} catch (e: Throwable) {
				createNonExistsStat(path)
			}
		}
	}
}

object JsUtils {
	suspend fun readRangeBytes(url: String, start: Double, end: Double): ByteArray = if (OS.isNodejs) {
		NodeJsUtils.readRangeBytes(url, start, end)
	} else {
		BrowserJsUtils.readRangeBytes(url, start, end)
	}

	suspend fun stat(url: String): JsStat = if (OS.isNodejs) {
		NodeJsUtils.httpStat(url)
	} else {
		BrowserJsUtils.stat(url)
	}

	suspend fun readBytes(url: String): ByteArray = readRangeBytes(url, -1.0, -1.0)
}

@JTranscMethodBody(target = "js", value = """
		var out = {};
		for (var n = 0; n < p0.length; n++) {
			var item = p0.data[n];
			out[N.istr(item["{% FIELD kotlin.Pair:first %}"])] = N.unbox(item["{% FIELD kotlin.Pair:second %}"]);
		}
		return out
	""")
external fun jsObject(vararg items: Pair<String, Any?>): JsDynamic?

fun jsObject(map: Map<String, Any?>): JsDynamic? = jsObject(*map.map { it.key to it.value }.toTypedArray())

@JTranscMethodBody(target = "js", value = "return require(N.istr(p0));")
external fun jsRequire(name: String): JsDynamic?

@JTranscMethodBody(target = "js", value = """
	return function(p1, p2, p3) {return N.unbox(p0['{% METHOD kotlin.jvm.functions.Function3:invoke %}'](p1, p2, p3));};
""")
external fun <TR> jsFunctionRaw3(v: Function3<JsDynamic?, JsDynamic?, JsDynamic?, TR>): JsDynamic?

@JTranscMethodBody(target = "js", value = """
	return function(p1, p2, p3, p4) {return N.unbox(p0['{% METHOD kotlin.jvm.functions.Function4:invoke %}'](p1, p2, p3, p4));};
""")
external fun <TR> jsFunctionRaw4(v: Function4<JsDynamic?, JsDynamic?, JsDynamic?, JsDynamic?, TR>): JsDynamic?

@JTranscMethodBody(target = "js", value = """return !!p0;""")
external fun JsDynamic?.toBool(): Boolean

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

object NodeJsUtils {
	suspend fun readRangeBytes(path: String, start: Double, end: Double): ByteArray = suspendCancellableCoroutine { c ->
		val http = jsRequire("http")
		val url = jsRequire("url")
		val info = url.call("parse", path)
		val headers = jsObject()

		if (start >= 0 && end >= 0) headers["Range"] = "bytes=$start-$end"

		val r = http.call("get", jsObject(
				"host" to info["hostname"],
				"port" to info["port"],
				"path" to info["path"],
				"agent" to false,
				"encoding" to null,
				"headers" to headers
		), jsFunctionRaw1 { res ->
			val body = jsArray()
			res.call("on", "data", jsFunctionRaw1 { d -> body.call("push", d) })
			res.call("on", "end", jsFunctionRaw0 {
				val r = global["Buffer"].call("concat", body)
				val u8array = jsNew("Int8Array", r)
				val out = ByteArray(r["length"].toInt())
				out.asJsDynamic().call("setArraySlice", 0, u8array)
				c.resume(out)
			})
		}).call("on", "error", jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error: ${e.toJavaString()}"))
		})

		c.onCancel {
			r.call("abort")
		}
	}

	suspend fun httpStat(path: String): JsStat = suspendCancellableCoroutine { c ->
		val http = jsRequire("http")
		val url = jsRequire("url")
		val info = url.call("parse", path)

		val r = http.call("get", jsObject(
				"method" to "HEAD",
				"host" to info["hostname"],
				"port" to info["port"],
				"path" to info["path"]
		), jsFunctionRaw1 { res ->
			val len = global.call("parseFloat", res["headers"]["content-length"])
			val out = JsStat(len.toDouble())
			c.resume(out)
		}.call("on", "error", jsFunctionRaw1 { e ->
			c.resumeWithException(RuntimeException("Error: ${e.toJavaString()}"))
		}))

		c.onCancel {
			r.call("abort")
		}

	}

}
*/
