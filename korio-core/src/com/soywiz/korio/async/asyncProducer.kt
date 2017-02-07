package com.soywiz.korio.async

import com.soywiz.korio.coroutine.korioStartCoroutine
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncOutputStream
import com.soywiz.korio.util.BYTES_EMPTY
import java.util.*
import java.util.concurrent.CancellationException

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

	suspend override fun consume(): T = korioSuspendCoroutine { c ->
		consumers += { c.resume(it) }
		flush()
	}

	suspend override fun consumeWithCancelHandler(cancel: CancelHandler): T = korioSuspendCoroutine { c ->
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

	callback.korioStartCoroutine(p, completion = EmptyContinuation)
	return p
}

fun Producer<ByteArray>.toAsyncOutputStream() = AsyncProducerStream(this)
fun Consumer<ByteArray>.toAsyncInputStream() = AsyncConsumerStream(this)

class AsyncProducerStream(val producer: Producer<ByteArray>) : AsyncOutputStream {
	suspend override fun write(buffer: ByteArray, offset: Int, len: Int) {
		TODO()
	}
}

class AsyncConsumerStream(val consumer: Consumer<ByteArray>) : AsyncInputStream {
	var current = BYTES_EMPTY
	var currentPos = 0
	val available get() = current.size - currentPos

	suspend private fun ensureNonEmptyBuffer() {
		while (available == 0) {
			currentPos = 0
			current = consumer.consume()
		}
	}

	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		ensureNonEmptyBuffer()
		val actualRead = Math.min(len, available)
		System.arraycopy(current, currentPos, buffer, offset, actualRead)
		currentPos += actualRead
		return actualRead
	}
}