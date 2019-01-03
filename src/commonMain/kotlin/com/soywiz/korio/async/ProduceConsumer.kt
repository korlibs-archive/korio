package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.concurrent.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
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
	private val items = Deque<T?>()
	private val consumers = Deque<(T?) -> Unit>()
	private var closed = false
	private val lock = Lock()

	val availableCount get() = lock { items.size }

	override fun produce(v: T) {
		lock { items.addLast(v) }
		flush()
	}

	override fun close() {
		lock {
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
			lock {
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
				lock {
					consumers -= consumer
				}
				c.resumeWithException(CancellationException(""))
			}
		}
		lock {
			consumers += consumer
		}
		flush()
	}
}

fun <T> asyncProducer(context: CoroutineContext, callback: suspend Producer<T>.() -> Unit): Consumer<T> =
	ProduceConsumer<T>().apply {
		launchAsap(context) {
			callback(this@apply)
		}
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