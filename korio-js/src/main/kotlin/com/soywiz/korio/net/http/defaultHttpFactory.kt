package com.soywiz.korio.net.http

import com.soywiz.korio.async.Promise
import com.soywiz.korio.global
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.OS
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType

impl var defaultHttpFactory: HttpFactory = object : HttpFactory() {
	override fun createClient(): HttpClient {
		return if (OS.isNodejs) HttpClientNodeJs() else HttpClientBrowserJs()
	}

	override fun createServer(): HttpServer = HttpSeverNodeJs()
}

class HttpSeverNodeJs : HttpServer() {
	// @TODO: Implement in nodejs!
}

fun jsRequire(name: String) = global.require(name)

class HttpClientNodeJs : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		//println(url)

		val http = jsRequire("http")
		val jsurl = jsRequire("url")
		val info = jsurl.call("parse", url)
		val reqHeaders = js("{}")

		for (header in headers) reqHeaders[header.first] = header.second

		val req = js("{}")
		req.method = method.name
		req.host = info["hostname"]
		req.port = info["port"]
		req.path = info["path"]
		req.agent = false
		req.encoding = null
		req.headers = reqHeaders

		val r = http.request(req, { res ->
			val statusCode = res.statusCode.toInt()
			val statusMessage = res.statusMessage.toJavaStringOrNull() ?: ""
			val jsHeadersObj = res.headers
			val body = js("[]")
			res.call("on", "data", { d -> body.call("push", d) })
			res.call("on", "end", {
				val r = global["Buffer"].call("concat", body)
				val u8array = Int8Array(r as ArrayBuffer)
				val out = ByteArray(u8array.length)
				for (n in 0 until u8array.length) out[n] = u8array[n]

				deferred.resolve(Response(
					status = statusCode,
					statusText = statusMessage,
					headers = Http.Headers(
						hashMapOf()
						//(jsHeadersObj?.toObjectMap() ?: HashMap()).mapValues { it.value.toJavaStringOrNull() ?: "" }
					),
					content = out.openAsync()
				))
			})
		}).on("error", { e ->
			deferred.reject(RuntimeException("Error: ${e.toJavaString()}"))
		})

		deferred.onCancel {
			r.abort()
		}

		if (content != null) {
			r.end(content.readAll().toTypedArray())
		} else {
			r.end()
		}
		Unit
	}
}

class HttpClientBrowserJs : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		val xhr = XMLHttpRequest()
		xhr.open(method.name, url, true)
		xhr.responseType = XMLHttpRequestResponseType.ARRAYBUFFER

		xhr.onload = { e ->
			val u8array = Uint8Array(xhr.response as ArrayBuffer)
			val out = ByteArray(u8array.length)
			out.asDynamic()["data"].set(u8array)
			deferred.resolve(Response(
				status = xhr.status.toInt(),
				statusText = xhr.statusText ?: "",
				headers = Http.Headers(xhr.getAllResponseHeaders()),
				content = out.openAsync()
			))
		}

		xhr.onerror = { e ->
			deferred.reject(RuntimeException("Error ${xhr.status} opening $url"))
		}

		for (header in headers) xhr.setRequestHeader(header.first, header.second)

		deferred.onCancel { xhr.abort() }

		if (content != null) {
			xhr.send(content.readAll())
		} else {
			xhr.send()
		}
	}
}

