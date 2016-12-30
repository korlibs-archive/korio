package com.soywiz.korio.vfs.js

import com.soywiz.korio.util.JsMethodBody
import kotlin.coroutines.CoroutineIntrinsics

object NodeJsUtils {
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
	external suspend fun readURLNodeJs(url: String): ByteArray

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}