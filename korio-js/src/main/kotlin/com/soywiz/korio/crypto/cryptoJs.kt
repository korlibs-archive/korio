package com.soywiz.korio.crypto

import com.soywiz.korio.async.suspendCoroutineEL
import com.soywiz.korio.ds.ByteArrayBuilder
import com.soywiz.korio.util.ByteArrayBuffer
import com.soywiz.korio.util.OS
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.get
import kotlin.browser.window
import kotlin.js.Promise

actual class SimplerMessageDigest actual constructor(name: String) {
	private val impl = if (OS.isBrowserJs) SimplerMessageDigestBrowser(name) else SimplerMessageDigestNode(name)

	actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = impl.update(data, offset, size)
	actual suspend fun digest(): ByteArray = impl.complete()
}

actual class SimplerMac actual constructor(name: String, key: ByteArray) {
	private val impl = if (OS.isBrowserJs) SimplerMacBrowser(name) else SimplerMacNode(name)

	actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = impl.update(data, offset, size)
	actual suspend fun finalize(): ByteArray = impl.complete()
}

private interface SimplerBase {
	suspend fun update(data: ByteArray, offset: Int, size: Int): Unit
	suspend fun complete(): ByteArray
}

private class SimplerMessageDigestNode(val name: String) : SimplerBase {
	override suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	override suspend fun complete(): ByteArray = TODO()
}

external interface CryptoSubtle {
	fun digest(algo: String, buffer: ByteArray): Promise<ArrayBuffer>
}

external interface Crypto {
	val subtle: CryptoSubtle
}

val crypto: Crypto get() = window["crypto"].asDynamic()

private class SimplerMessageDigestBrowser(val name: String) : SimplerBase {
	val buffer = ByteArrayBuilder()

	override suspend fun update(data: ByteArray, offset: Int, size: Int): Unit {
		buffer.append(data, offset, size)
	}
	override suspend fun complete(): ByteArray = suspendCoroutineEL { c ->
		crypto.subtle.digest(name, buffer.toByteArray()).then({
			c.resume(Int8Array(it).asDynamic())
		}, {
			c.resumeWithException(it)
		})
	}
}

private class SimplerMacNode(val name: String) : SimplerBase {
	override suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	override suspend fun complete(): ByteArray = TODO()
}

private class SimplerMacBrowser(val name: String) : SimplerBase {
	override suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	override suspend fun complete(): ByteArray = TODO()
}