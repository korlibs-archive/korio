package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.intrinsics.*
import kotlin.coroutines.*
import kotlin.math.*

typealias CancelHandler = Signal<Unit>

interface Consumer<T> : Closeable {
	suspend fun consume(cancel: CancelHandler? = null): T?
}

interface Producer<T> : Closeable {
	fun produce(v: T): Unit
}

open class ProduceConsumer<T> : Consumer<T>, Producer<T> {
	private val items = LinkedList<T?>()
	private val consumers = LinkedList<(T?) -> Unit>()
	private var closed = false

	val availableCount get() = synchronized2(this) { items.size }

	override fun produce(v: T) {
		synchronized2(this) { items.addLast(v) }
		flush()
	}

	override fun close() {
		synchronized2(this) {
			items.addLast(null)
			closed = true
		}
		flush()
	}

	private fun flush() {
		while (true) {
			var done = false
			var consumer: ((T?) -> Unit)? = null
			var item: T? = null
			synchronized2(this) {
				if (consumers.isNotEmpty() && items.isNotEmpty()) {
					consumer = consumers.removeFirst()
					item = items.removeFirst()
				} else {
					done = true
				}
			}
			if (done) break
			consumer!!(item)
		}
	}

	override suspend fun consume(cancel: CancelHandler?): T? = suspendCancellableCoroutine { c ->
		val consumer: (T?) -> Unit = {
			c.resume(it)
			//if (it != null) c.resume(it) else c.resumeWithException(EOFException())
		}
		if (cancel != null) {
			cancel {
				synchronized2(this) {
					consumers -= consumer
				}
				c.resumeWithException(CancellationException(""))
			}
		}
		synchronized2(this) {
			consumers += consumer
		}
		flush()
	}
}

fun <T> asyncProducer(context: CoroutineContext, callback: suspend Producer<T>.() -> Unit): Consumer<T> {
	val p = ProduceConsumer<T>()

	callback.startCoroutineCancellable(p, completion = EmptyContinuation(context))
	return p
}

fun Producer<ByteArray>.toAsyncOutputStream() = AsyncProducerStream(this)
fun Consumer<ByteArray>.toAsyncInputStream() = AsyncConsumerStream(this)

class AsyncProducerStream(val producer: Producer<ByteArray>) : AsyncOutputStream {
	override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
		producer.produce(buffer.copyOfRange(offset, offset + len))
	}

	override suspend fun close() {
		producer.close()
	}
}

class AsyncConsumerStream(val consumer: Consumer<ByteArray>) : AsyncInputStream {
	var eof = false
	var current = BYTES_EMPTY
	var currentPos = 0
	val available get() = current.size - currentPos

	private suspend fun ensureNonEmptyBuffer() {
		while (available == 0) {
			currentPos = 0
			val item = consumer.consume()
			if (item != null) {
				current = item
			} else {
				current = BYTES_EMPTY
				eof = true
				break
			}
		}
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		ensureNonEmptyBuffer()
		if (eof) return -1
		val actualRead = min(len, available)
		arraycopy(current, currentPos, buffer, offset, actualRead)
		currentPos += actualRead
		return actualRead
	}

	suspend override fun close() {
		consumer.close()
	}
}