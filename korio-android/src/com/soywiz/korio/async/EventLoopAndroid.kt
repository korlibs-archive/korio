package com.soywiz.korio.async

import com.soywiz.korio.android.KorioAndroidContext
import java.io.Closeable

class EventLoopAndroid : EventLoop {
	override fun init(): Unit {
	}

	override fun setImmediate(handler: () -> Unit) {
		KorioAndroidContext.runOnUiThread(handler)
	}

	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		android.os.Handler().postDelayed({
			if (!cancelled) {
				KorioAndroidContext.runOnUiThread(callback)
			}
		}, ms.toLong())
		return Closeable { cancelled = true }
	}

	override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		fun step() {
			android.os.Handler().postDelayed({
				if (!cancelled) {
					step()
					KorioAndroidContext.runOnUiThread(callback)
				}
			}, ms.toLong())
		}

		step()

		return Closeable { cancelled = true }
	}
}
