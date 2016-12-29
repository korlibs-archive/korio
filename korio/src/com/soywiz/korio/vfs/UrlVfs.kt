package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.util.JsMethodBody
import com.soywiz.korio.util.OS
import java.net.URL
import kotlin.coroutines.CoroutineIntrinsics

fun UrlVfs(url: String): VfsFile = UrlVfsImpl(url).root
fun UrlVfs(url: URL): VfsFile = UrlVfsImpl(url.toString()).root

internal class UrlVfsImpl(val urlStr: String) : Vfs() {
	fun resolve(path: String) = URL("$urlStr/$path".trim('/'))

	@JsMethodBody("""
		return this['{% METHOD #CLASS:readFullyJs %}'](p0, p1);
	""")
	suspend override fun readFully(path: String): ByteArray = asyncFun {
		val s = resolve(path).openStream()
		s.readBytes()
	}

	@Suppress("unused")
	private suspend fun readFullyJs(path: String): ByteArray = asyncFun {
		if (OS.isNodejs) {
			readURLNodeJs(resolve(path).toString())
		} else {
			readURLBrowser(resolve(path).toString(), -1.0, -1.0)
		}
	}

	@JsMethodBody("""
        var path = N.istr(p0);
        var continuation = p1;

        var http = require('http');
        var url = require('url');
		var info = url.parse(path);
		//console.log(info);
        http.get({
            host: info.hostname,
            port: info.port,
            path: info.path,
            agent: false,
			encoding: null
        }, function (res) {
			var body = [];
			res.on('data', function(d) { body.push(d); });
			res.on('end', function() {
				var res = Buffer.concat(body);
				var u8array = new Uint8Array(res);
				var out = new JA_B(res.length);
				out.setArraySlice(0, u8array);
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
			});
        });

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external private suspend fun readURLNodeJs(url: String): ByteArray

	@JsMethodBody("""
        var path = N.istr(p0);
		var start = p1;
		var end = p2;
        var continuation = p3;

		//console.log('readURLBrowser:' + path + ',' + start + '-' + end);
		//console.log(continuation);
		//console.log('JsHtmlDownloader.downloadFileRange:' + path + ',' + start + '-' + end);

		var xhr = new XMLHttpRequest();
		xhr.open('GET', path, true);
		xhr.responseType = 'arraybuffer';

		xhr.onload = function(e) {
			var u8array = new Uint8Array(this.response);
			var out = new JA_B(u8array.length);
			out.setArraySlice(0, u8array);
			continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
		};

		xhr.onreadystatechange = function() {
			//console.log(xhr.readyState);
			//console.log(xhr.status);
		};

		xhr.onerror = function(e) {
			continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + xhr.status + " opening " + path));
		};

		if (start >= 0 && end >= 0) xhr.setRequestHeader('Range', 'bytes=' + start + '-' + end);
		xhr.send();

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external private suspend fun readURLBrowser(url: String, start: Double, end: Double): ByteArray

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}