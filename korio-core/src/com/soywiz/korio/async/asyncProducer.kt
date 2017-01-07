package com.soywiz.korio.async

import java.util.*
import java.util.concurrent.CancellationException
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

typealias CancelHandler = Signal<Unit>

interface Consumer<T> {
	suspend fun consume(): T
	suspend fun consumeWithCancelHandler(cancel: CancelHandler): T
}

interface Producer<T> {
	fun produce(v: T): Unit
}

class ProduceConsumer<T> : Consumer<T>, Producer<T> {
	val items = LinkedList<T>()
	val consumers = LinkedList<(T) -> Unit>()

	override fun produce(v: T) {
		items.addLast(v)
		flush()
	}

	private fun flush() {
		while (items.isNotEmpty() && consumers.isNotEmpty()) {
			val consumer = consumers.removeFirst()
			val item = items.removeFirst()
			consumer(item)
		}
	}

	suspend override fun consume(): T = suspendCoroutine { c ->
		consumers += { c.resume(it) }
		flush()
	}

	suspend override fun consumeWithCancelHandler(cancel: CancelHandler): T = suspendCoroutine { c ->
		val consumer: (T) -> Unit = { c.resume(it) }
		cancel {
			consumers -= consumer
			c.resumeWithException(CancellationException())
		}
		consumers += consumer
		flush()
	}

}

fun <T> asyncProducer(callback: suspend Producer<T>.() -> Unit): Consumer<T> {
	val p = ProduceConsumer<T>()

	callback.startCoroutine(p, completion = EmptyContinuation)
	return p
}
