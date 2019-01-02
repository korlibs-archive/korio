@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.korio.*
import com.soywiz.korio.concurrent.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

interface SuspendingSequenceBuilder<in T> {
	suspend fun yield(value: T)
}

interface SuspendingSequence<out T> {
	operator fun iterator(): SuspendingIterator<T>
}

interface SuspendingSuspendSequence<out T> {
	suspend operator fun iterator(): SuspendingIterator<T>
}

interface SuspendingIterator<out T> {
	suspend operator fun hasNext(): Boolean
	suspend operator fun next(): T
}

fun <T> suspendingSequence(
	context: CoroutineContext,
	block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): SuspendingSequence<T> = object : SuspendingSequence<T> {
	override fun iterator(): SuspendingIterator<T> = suspendingIterator(context, block)
}

fun <T> suspendingIterator(
	context: CoroutineContext,
	block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): SuspendingIterator<T> = SuspendingIteratorCoroutine<T>(context).apply {
	nextStep = block.createCoroutine(receiver = this, completion = this)
}

class SuspendingIteratorCoroutine<T>(
	override val context: CoroutineContext
) : SuspendingIterator<T>, SuspendingSequenceBuilder<T>, Continuation<Unit> {
	enum class State { INITIAL, COMPUTING_HAS_NEXT, COMPUTING_NEXT, COMPUTED, DONE }

	var state: State = State.INITIAL
	var nextValue: T? = null
	var nextStep: Continuation<Unit>? = null // null when sequence complete

	// if (state == COMPUTING_NEXT) computeContinuation is Continuation<T>
	// if (state == COMPUTING_HAS_NEXT) computeContinuation is Continuation<Boolean>
	var computeContinuation: Continuation<*>? = null

	suspend fun computeHasNext(): Boolean = suspendCancellableCoroutine { c ->
		state = State.COMPUTING_HAS_NEXT
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	suspend fun computeNext(): T = suspendCancellableCoroutine { c ->
		state = State.COMPUTING_NEXT
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	override suspend fun hasNext(): Boolean = when (state) {
		State.INITIAL -> computeHasNext()
		State.COMPUTED -> true
		State.DONE -> false
		else -> throw IllegalStateException("Recursive dependency detected -- already computing next")
	}

	override suspend fun next(): T = when (state) {
		State.INITIAL -> computeNext()
		State.COMPUTED -> {
			state = State.INITIAL
			nextValue as T
		}
		State.DONE -> throw NoSuchElementException()
		else -> throw IllegalStateException("Recursive dependency detected -- already computing next")
	}

	@Suppress("UNCHECKED_CAST")
	fun resumeIterator(hasNext: Boolean) {
		when (state) {
			State.COMPUTING_HAS_NEXT -> {
				state = State.COMPUTED
				(computeContinuation as Continuation<Boolean>).resume(hasNext)
			}
			State.COMPUTING_NEXT -> {
				state = State.INITIAL
				(computeContinuation as Continuation<T>).resume(nextValue as T)
			}
			else -> throw IllegalStateException("Was not supposed to be computing next value. Spurious yield?")
		}
	}

	override fun resumeWith(result: Result<Unit>) {
		val exception = result.exceptionOrNull()
		if (exception != null) {
			nextStep = null
			state = State.DONE
			computeContinuation!!.resumeWithException(exception)
		} else {
			nextStep = null
			resumeIterator(false)
		}
	}

	// Generator implementation
	override suspend fun yield(value: T): Unit = suspendCancellableCoroutine { c ->
		nextValue = value
		nextStep = c
		resumeIterator(true)
	}
}

fun <T> Iterator<T>.toAsync(): SuspendingIterator<T> = object : SuspendingIterator<T> {
	override suspend fun hasNext(): Boolean = this@toAsync.hasNext()
	override suspend fun next(): T = this@toAsync.next()
}

fun <T> SuspendingSequence(items: Iterable<T>) = items.toAsync()

fun <T> Iterable<T>.toAsync(): SuspendingSequence<T> = object : SuspendingSequence<T> {
	override fun iterator(): SuspendingIterator<T> = this@toAsync.iterator().toAsync()
}

fun <T> asyncGenerate(
	context: CoroutineContext,
	block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): SuspendingSequence<T> = object : SuspendingSequence<T> {
	override fun iterator(): SuspendingIterator<T> = suspendingIterator(context, block)
}

suspend fun <T> asyncGenerate(block: suspend SuspendingSequenceBuilder<T>.() -> Unit): SuspendingSequence<T> =
	asyncGenerate(coroutineContext, block)

suspend inline fun <T, T2> SuspendingSequence<T>.map(crossinline transform: (T) -> T2) = asyncGenerate<T2> {
	for (e in this@map) {
		yield(transform(e))
	}
}

suspend inline fun <T> SuspendingSequence<T>.filter(crossinline filter: (T) -> Boolean) = asyncGenerate<T> {
	for (e in this@filter) {
		if (filter(e)) {
			yield(e)
		}
	}
}


suspend fun <T> SuspendingSequence<T>.chunks(count: Int) = asyncGenerate<List<T>> {
	val chunk = arrayListOf<T>()

	for (e in this@chunks) {
		chunk += e
		if (chunk.size > count) {
			yield(chunk.toList())
			chunk.clear()
		}
	}

	if (chunk.size > 0) {
		yield(chunk.toList())
	}
}


suspend fun <T> SuspendingSequence<T>.toList(): List<T> {
	val out = arrayListOf<T>()
	for (e in this@toList) out += e
	return out
}

inline suspend fun <T, TR> SuspendingSequence<T>.fold(initial: TR, crossinline folder: (T, TR) -> TR): TR {
	var result: TR = initial
	for (e in this) result = folder(e, result)
	return result
}

suspend fun SuspendingSequence<Int>.sum(): Int = this.fold(0) { a, b -> a + b }


suspend fun <T> SuspendingSequence<T>.isEmpty(): Boolean {
	var hasItems = false
	for (e in this@isEmpty) {
		hasItems = true
		break
	}
	return hasItems
}

suspend fun <T> SuspendingSequence<T>.isNotEmpty(): Boolean {
	var hasItems = false
	for (e in this@isNotEmpty) {
		hasItems = true
		break
	}
	return !hasItems
}

suspend fun <T : Any?> SuspendingSequence<T>.firstOrNull(): T? {
	var result: T? = null
	for (e in this) {
		result = e
		break
	}
	return result
}

suspend fun <T : Any?> SuspendingSequence<T>.take(count: Int): SuspendingSequence<T> = asyncGenerate {
	var current = 0
	val iterator = iterator()
	while (current < count && iterator.hasNext()) {
		yield(iterator.next())
		current++
	}
}

class AsyncSequenceEmitter<T : Any> : Extra by Extra.Mixin() {
	private val signal = Signal<Unit>()
	private var queuedElements = ArrayList<T>()
	private val lock = Lock()
	private var closed = false

	fun close() {
		closed = true
		signal()
	}

	fun emit(v: T) {
		lock { queuedElements.add(v) }
		signal()
	}

	operator fun invoke(v: T) = emit(v)

	fun toSequence(): SuspendingSequence<T> = object : SuspendingSequence<T> {
		override fun iterator(): SuspendingIterator<T> = object : SuspendingIterator<T> {
			override suspend fun hasNext(): Boolean {
				while (lock { queuedElements.isEmpty() && !closed }) signal.waitOne()
				return queuedElements.isNotEmpty() || !closed
			}

			override suspend fun next(): T {
				while (lock { queuedElements.isEmpty() && !closed }) signal.waitOne()
				if (queuedElements.isEmpty() && closed) throw RuntimeException("Already closed")
				return lock { queuedElements.removeAt(queuedElements.size - 1) }
			}
		}
	}
}

class SuspendingSequenceBuilder2<T : Any> {
	val emitter = AsyncSequenceEmitter<T>()
	fun yield(value: T) = emitter.emit(value)
	fun close() = emitter.close()
}

interface SuspendingSequence2<out T> {
	suspend operator fun iterator(): SuspendingIterator<T>
}

suspend fun <T : Any> asyncGenerate2(
	block: suspend SuspendingSequenceBuilder2<T>.() -> Unit
): SuspendingSequence2<T> = object : SuspendingSequence2<T> {
	override suspend fun iterator(): SuspendingIterator<T> {
		val builder = SuspendingSequenceBuilder2<T>()
		block(builder)
		return builder.emitter.toSequence().iterator()
	}
}

fun <T : Any> asyncGenerate3(
	block: SuspendingSequenceBuilder2<T>.() -> Unit
): SuspendingSequence<T> = object : SuspendingSequence<T> {
	override fun iterator(): SuspendingIterator<T> {
		val builder = SuspendingSequenceBuilder2<T>()
		block(builder)
		return builder.emitter.toSequence().iterator()
	}
}
