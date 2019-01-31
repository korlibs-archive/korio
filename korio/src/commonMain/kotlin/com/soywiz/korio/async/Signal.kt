@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.klock.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*


class AsyncSignal<T>(val onRegister: () -> Unit = {}) { //: AsyncSequence<T> {
	inner class Node(val once: Boolean, val item: suspend (T) -> Unit) : Closeable {
		override fun close() {
			handlers.remove(this)
		}
	}

	private var handlers = ArrayList<Node>()

	val listenerCount: Int get() = handlers.size

	fun once(handler: suspend (T) -> Unit): Closeable = _add(true, handler)
	fun add(handler: suspend (T) -> Unit): Closeable = _add(false, handler)

	private fun _add(once: Boolean, handler: suspend (T) -> Unit): Closeable {
		onRegister()
		val node = Node(once, handler)
		handlers.add(node)
		return node
	}

	suspend operator fun invoke(value: T) {
		val it = handlers.iterator()
		while (it.hasNext()) {
			val node = it.next()
			if (node.once) it.remove()
			node.item(value)
		}
	}

	operator fun invoke(handler: suspend (T) -> Unit): Closeable = add(handler)

	suspend fun listen(): ReceiveChannel<T> = produce {
		while (true) {
			send(waitOne())
		}
	}
}

fun <TI, TO> AsyncSignal<TI>.mapSignal(transform: (TI) -> TO): AsyncSignal<TO> {
	val out = AsyncSignal<TO>()
	this.add { out(transform(it)) }
	return out
}

suspend operator fun AsyncSignal<Unit>.invoke() = invoke(Unit)

suspend fun <T> AsyncSignal<T>.waitOne(): T = suspendCancellableCoroutine { c ->
	var close: Closeable? = null
	close = once {
		close?.close()
		c.resume(it)
	}
	c.invokeOnCancellation {
		close.close()
	}
}

//////////////////////////////////

class Signal<T>(val onRegister: () -> Unit = {}) { //: AsyncSequence<T> {
	inner class Node(val once: Boolean, val item: (T) -> Unit) : Closeable {
		override fun close() {
			handlers.remove(this)
		}
	}

	private var handlersToRun = ArrayList<Node>()
	private var handlers = ArrayList<Node>()
	private var handlersNoOnce = ArrayList<Node>()

	val listenerCount: Int get() = handlers.size

	fun once(handler: (T) -> Unit): Closeable = _add(true, handler)
	fun add(handler: (T) -> Unit): Closeable = _add(false, handler)

	fun clear() = handlers.clear()

	private fun _add(once: Boolean, handler: (T) -> Unit): Closeable {
		onRegister()
		val node = Node(once, handler)
		handlers.add(node)
		return node
	}

	operator fun invoke(value: T) {
		val oldHandlers = handlers
		handlersNoOnce.clear()
		handlersToRun.clear()
		for (handler in oldHandlers) {
			handlersToRun.add(handler)
			if (!handler.once) handlersNoOnce.add(handler)
		}
		val temp = handlers
		handlers = handlersNoOnce
		handlersNoOnce = temp

		for (handler in handlersToRun) {
			handler.item(value)
		}
	}

	operator fun invoke(handler: (T) -> Unit): Closeable = add(handler)

	suspend fun listen(): ReceiveChannel<T> = produce {
		while (true) {
			send(waitOne())
		}
	}


//override fun iterator(): AsyncIterator<T> = asyncGenerate {
//	while (true) {
//		yield(waitOne())
//	}
//}.iterator()
}

//class AsyncSignal<T>(context: CoroutineContext) {

//}

fun <TI, TO> Signal<TI>.mapSignal(transform: (TI) -> TO): Signal<TO> {
	val out = Signal<TO>()
	this.add { out(transform(it)) }
	return out
}

operator fun Signal<Unit>.invoke() = invoke(Unit)

suspend fun Iterable<Signal<*>>.waitOne(): Any? = suspendCancellableCoroutine { c ->
	val closes = arrayListOf<Closeable>()
	for (signal in this) {
		closes += signal.once {
			closes.close()
			c.resume(it)
		}
	}

	c.invokeOnCancellation {
		closes.close()
	}
}

fun <T> Signal<T>.waitOnePromise(): Deferred<T> {
	val deferred = CompletableDeferred<T>(Job())
	var close: Closeable? = null
	close = once {
		close?.close()
		deferred.complete(it)
	}
	deferred.invokeOnCompletion {
		close.close()
	}
	return deferred
}

suspend fun <T> Signal<T>.addSuspend(handler: suspend (T) -> Unit): Closeable {
	val cc = coroutineContext
	return this@addSuspend { value ->
		launchImmediately(cc) {
			handler(value)
		}
	}
}

fun <T> Signal<T>.addSuspend(context: CoroutineContext, handler: suspend (T) -> Unit): Closeable =
	this@addSuspend { value ->
		launchImmediately(context) {
			handler(value)
		}
	}


suspend fun <T> Signal<T>.waitOne(): T = suspendCancellableCoroutine { c ->
	var close: Closeable? = null
	close = once {
		close?.close()
		c.resume(it)
	}
	c.invokeOnCancellation {
		close?.close()
	}
}

suspend fun <T> Signal<T>.waitOne(timeout: TimeSpan): T? = kotlinx.coroutines.suspendCancellableCoroutine { c ->
	var close: Closeable? = null
	var running = true

	fun closeAll() {
		running = false
		close?.close()
	}

	launchImmediately(c.context) {
		delay(timeout)
		if (running) {
			closeAll()
			c.resume(null)
		}
	}

	close = once {
		closeAll()
		c.resume(it)
	}

	c.invokeOnCancellation {
		closeAll()
	}
}

suspend fun <T> Signal<T>.waitOneOpt(timeout: TimeSpan?): T? = when {
	timeout != null -> waitOne(timeout)
	else -> waitOne()
}

suspend inline fun <T> Map<Signal<Unit>, T>.executeAndWaitAnySignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeables = this.map { pair -> pair.key.once { deferred.complete(pair.value) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

suspend inline fun <T> Iterable<Signal<T>>.executeAndWaitAnySignal(callback: () -> Unit): Pair<Signal<T>, T> {
	val deferred = CompletableDeferred<Pair<Signal<T>, T>>()
	val closeables = this.map { signal -> signal.once { deferred.complete(signal to it) } }
	try {
		callback()
		return deferred.await()
	} finally {
		closeables.close()
	}
}

suspend inline fun <T> Signal<T>.executeAndWaitSignal(callback: () -> Unit): T {
	val deferred = CompletableDeferred<T>()
	val closeable = this.once { deferred.complete(it) }
	try {
		callback()
		return deferred.await()
	} finally {
		closeable.close()
	}
}
