@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.util.Extra
import java.util.*
import kotlin.coroutines.*

interface SuspendingSequenceBuilder<in T> {
	suspend fun yield(value: T)
}

interface SuspendingSequence<out T> {
	operator fun iterator(): SuspendingIterator<T>
}

interface SuspendingIterator<out T> {
	suspend operator fun hasNext(): Boolean
	suspend operator fun next(): T
}

fun <T> suspendingSequence(
	context: CoroutineContext = EmptyCoroutineContext,
	block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): SuspendingSequence<T> = object : SuspendingSequence<T> {
	override fun iterator(): SuspendingIterator<T> = suspendingIterator(context, block)

}

fun <T> suspendingIterator(
	context: CoroutineContext = EmptyCoroutineContext,
	block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): SuspendingIterator<T> = SuspendingIteratorCoroutine<T>(context).apply {
	nextStep = block.createCoroutine(receiver = this, completion = this)
}

class SuspendingIteratorCoroutine<T>(
	override val context: CoroutineContext
): SuspendingIterator<T>, SuspendingSequenceBuilder<T>, Continuation<Unit> {
	enum class State { INITIAL, COMPUTING_HAS_NEXT, COMPUTING_NEXT, COMPUTED, DONE }
	var state: State = State.INITIAL
	var nextValue: T? = null
	var nextStep: Continuation<Unit>? = null // null when sequence complete

	// if (state == COMPUTING_NEXT) computeContinuation is Continuation<T>
	// if (state == COMPUTING_HAS_NEXT) computeContinuation is Continuation<Boolean>
	var computeContinuation: Continuation<*>? = null

	suspend fun computeHasNext(): Boolean = suspendCoroutine { c ->
		state = State.COMPUTING_HAS_NEXT
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	suspend fun computeNext(): T = suspendCoroutine { c ->
		state = State.COMPUTING_NEXT
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	override suspend fun hasNext(): Boolean {
		when (state) {
			State.INITIAL -> return computeHasNext()
			State.COMPUTED -> return true
			State.DONE -> return false
			else -> throw IllegalStateException("Recursive dependency detected -- already computing next")
		}
	}

	override suspend fun next(): T {
		when (state) {
			State.INITIAL -> return computeNext()
			State.COMPUTED -> {
				state = State.INITIAL
				return nextValue as T
			}
			State.DONE -> throw NoSuchElementException()
			else -> throw IllegalStateException("Recursive dependency detected -- already computing next")
		}
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

	// Completion continuation implementation
	override fun resume(value: Unit) {
		nextStep = null
		resumeIterator(false)
	}

	override fun resumeWithException(exception: Throwable) {
		nextStep = null
		state = State.DONE
		computeContinuation!!.resumeWithException(exception)
	}

	// Generator implementation
	override suspend fun yield(value: T): Unit = suspendCoroutine { c ->
		nextValue = value
		nextStep = c
		resumeIterator(true)
	}
}

// @TODO: @BUG: https://youtrack.jetbrains.com/issue/KT-15828#u=1484838346426
typealias AsyncGenerator<T> = SuspendingSequenceBuilder<T>
typealias AsyncSequence<T> = SuspendingSequence<T>
typealias AsyncIterator<T> = SuspendingIterator<T>

fun <T> asyncGenerate(
	context: CoroutineContext = EmptyCoroutineContext,
	block: suspend SuspendingSequenceBuilder<T>.() -> Unit
): SuspendingSequence<T> = object : SuspendingSequence<T> {
	override fun iterator(): SuspendingIterator<T> = suspendingIterator(context, block)
}

//fun <T> asyncGenerate(
//	context: CoroutineContext = EmptyCoroutineContext,
//	block: suspend SuspendingSequenceBuilder<T>.() -> Unit
//): SuspendingIterator<T> = SuspendingIteratorCoroutine<T>(context).apply {
//	nextStep = block.createCoroutine(receiver = this, completion = this)
//}

/*
interface AsyncGenerator<in T> {
	suspend fun yield(value: T)
}

interface AsyncSequence<out T> {
	operator fun iterator(): AsyncIterator<T>
}

interface AsyncIterator<out T> {
	suspend operator fun hasNext(): Boolean
	suspend operator fun next(): T
}

fun <T> asyncGenerate(block: suspend AsyncGenerator<T>.() -> Unit): AsyncSequence<T> = object : AsyncSequence<T> {
	override fun iterator(): AsyncIterator<T> {
		val iterator = AsyncGeneratorIterator<T>()
		iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
		return iterator
	}
}

class AsyncGeneratorIterator<T> : AsyncIterator<T>, AsyncGenerator<T>, Continuation<Unit> {
	override val context: CoroutineContext = EmptyCoroutineContext

	enum class State { INITIAL, COMPUTING_HAS_NEXT, COMPUTING_NEXT, COMPUTED, DONE }

	var state: State = State.INITIAL
	var nextValue: T? = null
	var nextStep: Continuation<Unit>? = null // null when sequence complete

	// if (state == COMPUTING_NEXT) computeContinuation is Continuation<T>
	// if (state == COMPUTING_HAS_NEXT) computeContinuation is Continuation<Boolean>
	var computeContinuation: Continuation<*>? = null

	suspend fun computeHasNext(): Boolean = suspendCoroutine { c ->
		state = State.COMPUTING_HAS_NEXT
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	suspend fun computeNext(): T = suspendCoroutine { c ->
		state = State.COMPUTING_NEXT
		computeContinuation = c
		nextStep!!.resume(Unit)
	}

	override suspend fun hasNext(): Boolean {
		when (state) {
			State.INITIAL -> return computeHasNext()
			State.COMPUTED -> return true
			State.DONE -> return false
			else -> throw IllegalStateException("Recursive dependency detected -- already computing next")
		}
	}

	override suspend fun next(): T {
		when (state) {
			State.INITIAL -> return computeNext()
			State.COMPUTED -> {
				state = State.INITIAL
				return nextValue as T
			}
			State.DONE -> throw java.util.NoSuchElementException()
			else -> {
				throw IllegalStateException("Recursive dependency detected -- already computing next")
			}
		}
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

	// Completion continuation implementation
	override fun resume(value: Unit) {
		nextStep = null
		resumeIterator(false)
	}

	override fun resumeWithException(exception: Throwable) {
		nextStep = null
		state = State.DONE
		computeContinuation!!.resumeWithException(exception)
	}

	// Generator implementation
	override suspend fun yield(value: T): Unit = suspendCoroutine { c ->
		nextValue = value
		nextStep = c
		resumeIterator(true)
	}
}
*/

@PublishedApi
// @todo: @bug: https://youtrack.jetbrains.com/issue/KT-15820
internal inline suspend fun <T, T2> _map(seq: SuspendingSequence<T>, crossinline transform: (T) -> T2) = asyncGenerate<T2> {
	for (e in seq) {
		yield(transform(e))
	}
}

inline suspend fun <T, T2> SuspendingSequence<T>.map(crossinline transform: (T) -> T2) = _map(this, transform)

@PublishedApi
// @todo: @bug: https://youtrack.jetbrains.com/issue/KT-15820
internal inline suspend fun <T> _filter(it: SuspendingSequence<T>, crossinline filter: (T) -> Boolean) = asyncGenerate<T> {
	for (e in it) {
		if (filter(e)) {
			yield(e)
		}
	}
}

inline suspend fun <T> SuspendingSequence<T>.filter(crossinline filter: (T) -> Boolean) = _filter(this, filter)

@PublishedApi
// @todo: @bug: https://youtrack.jetbrains.com/issue/KT-15820
internal suspend fun <T> _chunks(it: SuspendingSequence<T>, count: Int) = asyncGenerate<List<T>> {
	val chunk = arrayListOf<T>()

	for (e in it) {
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

suspend fun <T> SuspendingSequence<T>.chunks(count: Int) = _chunks(this, count)

suspend fun <T> SuspendingSequence<T>.toList(): List<T> {
	val out = arrayListOf<T>()
	val it = this.iterator()
	while (it.hasNext()) {
		out += it.next()
	}
	return out
}

// @TODO: @BUG: https://youtrack.jetbrains.com/issue/KT-15824
//suspend fun <T> AsyncSequence<T>.toList(): List<T> {
//	val out = arrayListOf<T>()
//	for (e in this@toList) out += e
//	return out
//}

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

class AsyncSequenceEmitter<T : Any> : Extra by Extra.Mixin() {
	private val signal = Signal<Unit>()
	private var queuedElements = LinkedList<T>()
	private var closed = false

	fun close() {
		closed = true
		signal()
	}

	fun emit(v: T) {
		synchronized(queuedElements) { queuedElements.add(v) }
		signal()
	}

	operator fun invoke(v: T) = emit(v)

	fun toSequence(): SuspendingSequence<T> = object : SuspendingSequence<T> {
		override fun iterator(): AsyncIterator<T> = object : AsyncIterator<T> {
			suspend override fun hasNext(): Boolean {
				while (synchronized(queuedElements) { queuedElements.isEmpty() && !closed }) signal.waitOne()
				return queuedElements.isNotEmpty() || !closed
			}

			suspend override fun next(): T {
				while (synchronized(queuedElements) { queuedElements.isEmpty() && !closed }) signal.waitOne()
				if (queuedElements.isEmpty() && closed) throw RuntimeException("Already closed")
				return synchronized(queuedElements) { queuedElements.remove() }
			}
		}
	}
}
