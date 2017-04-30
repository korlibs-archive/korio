package com.soywiz.korio.async

import com.soywiz.korio.android.KorioAndroidContext
import java.io.Closeable

class EventLoopFactoryAndroid : EventLoopFactory() {
	override val priority: Int = 2000
	override val available: Boolean get() = System.getProperty("java.runtime.name").contains("android", ignoreCase = true)

	override fun createEventLoop(): EventLoop = EventLoopAndroid()
}

class EventLoopAndroid : EventLoop() {
	override fun setImmediateInternal(handler: () -> Unit) {
		KorioAndroidContext.runOnUiThread(handler)
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		android.os.Handler().postDelayed({
			if (!cancelled) {
				KorioAndroidContext.runOnUiThread(callback)
			}
		}, ms.toLong())
		return Closeable { cancelled = true }
	}

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
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
