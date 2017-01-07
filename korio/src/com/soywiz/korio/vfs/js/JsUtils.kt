package com.soywiz.korio.vfs.js

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.JsDynamic
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.util.OS

object JsUtils {
	suspend fun readRangeBytes(url: String, start: Double, end: Double): ByteArray = asyncFun {
		if (OS.isNodejs) {
			NodeJsUtils.readRangeBytes(url, start, end)
		} else {
			BrowserJsUtils.readRangeBytes(url, start, end)
		}
	}

	suspend fun stat(url: String): JsStat = asyncFun {
		if (OS.isNodejs) {
			NodeJsUtils.httpStat(url)
		} else {
			BrowserJsUtils.stat(url)
		}
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