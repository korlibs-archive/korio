package com.soywiz.korio.net.http

import com.jtransc.js.*
import com.soywiz.korio.async.Promise
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.OS

class HttpFactoryJs : HttpFactory() {
	override fun createClient(): HttpClient = if (OS.isNodejs) HttpClientNodeJs() else HttpClientBrowserJs()
}

class HttpClientNodeJs : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		println(url)

		val http = jsRequire("http")
		val jsurl = jsRequire("url")
		val info = jsurl.call("parse", url)
		val reqHeaders = jsObject()

		for (header in headers) reqHeaders[header.first] = header.second

		val r = http.call("request", jsObject(
			"method" to method.nameUC,
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
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		val xhr = jsNew("XMLHttpRequest")
		xhr.call("open", method.nameUC, url, true)
		xhr["responseType"] = "arraybuffer"

		xhr["onload"] = jsFunctionRaw1 { e ->
			val u8array = jsNew("Uint8Array", xhr["response"])
			val out = ByteArray(u8array["length"].toInt())
			out.asJsDynamic()["data"].call("set", u8array)
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
