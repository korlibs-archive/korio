package com.soywiz.korio.vfs.js

import com.jtransc.annotation.JTranscMethodBody
import kotlin.coroutines.CoroutineIntrinsics

object NodeJsUtils {
	@JTranscMethodBody(target = "js", value = """
        var path = N.istr(p0), start = p1, end = p2, continuation = p3;

        var http = require('http');
        var url = require('url');
		var info = url.parse(path);
		//console.log(info);
		var headers = {};

		if (start >= 0 && end >= 0) headers['Range'] = 'bytes=' + start + '-' + end;

        http.get({
            host: info.hostname,
            port: info.port,
            path: info.path,
            agent: false,
			encoding: null,
			headers: headers
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
	external suspend fun readRangeBytes(url: String, start: Double, end: Double): ByteArray

	@JTranscMethodBody(target = "js", value = """
        var path = N.istr(p0), continuation = p1;

        var http = require('http');
        var url = require('url');
		var info = url.parse(path);

        http.get({
			method: 'HEAD',
            host: info.hostname,
            port: info.port,
            path: info.path
        }, function (res) {
	        var len = parseFloat(res.headers['content-length']);
			var out = {% CONSTRUCTOR com.soywiz.korio.vfs.js.JsStat:(D)V %}(len);
			continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
        });

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun stat(url: String): JsStat

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}