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
				var u8array = new Int8Array(res);
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
	external suspend fun httpStat(url: String): JsStat

	@JTranscMethodBody(target = "js", value = """
        var path = N.istr(p0), mode = N.istr(p1), continuation = p2;

        var fs = require('fs');
		fs.open(path, mode, function(err, fd) {
			if (err) {
				continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + err + ' opening' + path));
			} else {
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](fd);
			}
		});

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun open(path: String, mode: String): Any

	@JTranscMethodBody(target = "js", value = """
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
        var fd = p0, position = p1, len = p2, continuation = p3;

        var fs = require('fs');
		var buffer = new Buffer(len);
		fs.read(fd, buffer, 0, len, position, function(err, bytesRead, buffer) {
			if (err) {
				continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + err + ' opening' + path));
			} else {

				var u8array = new Int8Array(buffer, 0, bytesRead);
				var out = new JA_B(bytesRead);
				out.setArraySlice(0, u8array);

				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
			}
		});

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun read(handle: Any, position: Double, length: Double): ByteArray

	@JTranscMethodBody(target = "js", value = """
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
        var fd = p0, continuation = p1;

        var fs = require('fs');
		fs.fstat(fd, function(err, stat) {
			if (err) {
				continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + err + ' opening' + path));
			} else {
				var out = {% CONSTRUCTOR com.soywiz.korio.vfs.js.JsStat:(D)V %}(+stat.size);
				out['{% FIELD com.soywiz.korio.vfs.js.JsStat:isDirectory %}'] = stat.isDirectory();
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
			}
		});

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun fsStat(handle: Any): JsStat

	@JTranscMethodBody(target = "js", value = """
        var fd = p0, continuation = p2;
        var fs = require('fs');
		fs.close(fd, mode, function(err, fd) {
			if (err) {
				continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + err + ' opening' + path));
			} else {
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](null);
			}
		});
		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun close(handle: Any): Unit

	@JTranscMethodBody(target = "js", value = "return N.str(process.cwd());")
	external fun getCWD(): String

	@JTranscMethodBody(target = "js", value = """
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
        var path = N.istr(p0), continuation = p1;

        var fs = require('fs');
		fs.stat(path, function(err, stat) {
			if (err) {
				continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + err + ' opening' + path));
			} else {
				var out = {% CONSTRUCTOR com.soywiz.korio.vfs.js.JsStat:(D)V %}(+stat.size);
				out['{% FIELD com.soywiz.korio.vfs.js.JsStat:isDirectory %}'] = stat.isDirectory();
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
			}
		});

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun fstat(path: String): JsStat

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}