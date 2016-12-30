package com.soywiz.korio.vfs.js

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.util.OS
import kotlin.coroutines.CoroutineIntrinsics

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
			NodeJsUtils.stat(url)
		} else {
			BrowserJsUtils.stat(url)
		}
	}

	suspend fun readBytes(url: String): ByteArray = readRangeBytes(url, -1.0, -1.0)
}