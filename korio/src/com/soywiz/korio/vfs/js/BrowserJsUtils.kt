package com.soywiz.korio.vfs.js

import com.jtransc.annotation.JTranscMethodBody
import kotlin.coroutines.CoroutineIntrinsics

object BrowserJsUtils {
	@JTranscMethodBody(target = "js", value = """
        var url = N.istr(p0), start = p1, end = p2, continuation = p3;

		var xhr = new XMLHttpRequest();
		xhr.open('GET', url, true);
		xhr.responseType = 'arraybuffer';

		xhr.onload = function(e) {
			var u8array = new Uint8Array(this.response);
			var out = new JA_B(u8array.length);
			out.setArraySlice(0, u8array);
			continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
		};

		xhr.onerror = function(e) {
			continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + xhr.status + " opening " + url));
		};

		if (start >= 0 && end >= 0) xhr.setRequestHeader('Range', 'bytes=' + start + '-' + end);
		xhr.send();

		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun readRangeBytes(url: String, start: Double, end: Double): ByteArray

	@JTranscMethodBody(target = "js", value = """
        var url = N.istr(p0), continuation = p1;

		var xhr = new XMLHttpRequest();
		xhr.open("HEAD", url, true);
		xhr.onreadystatechange = function() {
		    if (this.readyState == this.DONE) {
		        var len = parseFloat(xhr.getResponseHeader("Content-Length"));
				var out = {% CONSTRUCTOR com.soywiz.korio.vfs.js.JsStat:(D)V %}(len);
				continuation['{% METHOD kotlin.coroutines.Continuation:resume %}'](out);
		    }
		};
		xhr.onerror = function(e) {
			continuation['{% METHOD kotlin.coroutines.Continuation:resumeWithException %}'](N.createRuntimeException('Error ' + xhr.status + " opening " + path));
		};
		xhr.send();
		return this['{% METHOD #CLASS:getSuspended %}']();
    """)
	external suspend fun stat(url: String): JsStat

	@JTranscMethodBody(target = "js", value = """
		var baseHref = document.location.href.replace(/\/[^\/]*$/, '');
		var bases = document.getElementsByTagName('base');
		if (bases.length > 0) baseHref = bases[0].href;
		return N.str(baseHref);
	""")
	external fun getBaseUrl(): String

	@Suppress("unused")
	private fun getSuspended() = CoroutineIntrinsics.SUSPENDED
}
