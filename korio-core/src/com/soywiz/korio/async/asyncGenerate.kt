@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.util.Extra
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.suspendCoroutine

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

class AsyncSequenceEmitter<T : Any> : Extra by Extra.Mixin() {
	val signal = Signal<Unit>()
	var queuedElements = ConcurrentLinkedQueue<T>()
	var closed = false

	fun close() {
		closed = true
		signal()
	}

	fun emit(v: T) {
		queuedElements.add(v)
		signal()
	}

	operator fun invoke(v: T) = emit(v)

	fun toSequence(): AsyncSequence<T> = object : AsyncSequence<T> {
		override fun iterator(): AsyncIterator<T> = object : AsyncIterator<T> {
			suspend override fun hasNext(): Boolean = asyncFun {
				while (queuedElements.size == 0 && !closed) signal.waitOne()
				!closed
			}

			suspend override fun next(): T = asyncFun {
				while (queuedElements.size == 0 && !closed) signal.waitOne()
				if (closed) throw RuntimeException("Already closed")
				queuedElements.remove()
			}
		}
	}
}

fun <T> asyncGenerate(block: suspend AsyncGenerator<T>.() -> Unit): AsyncSequence<T> = object : AsyncSequence<T> {
	override fun iterator(): AsyncIterator<T> {
		val iterator = AsyncGeneratorIterator<T>()
		iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
		return iterator
	}
}

class AsyncGeneratorIterator<T> : AsyncIterator<T>, AsyncGenerator<T>, Continuation<Unit> {
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

inline suspend fun <T, T2> AsyncSequence<T>.map(crossinline transform: (T) -> T2) = asyncGenerate<T2> {
	for (e in this@map) {
		yield(transform(e))
	}
}

inline suspend fun <T> AsyncSequence<T>.filter(crossinline filter: (T) -> Boolean) = asyncGenerate<T> {
	for (e in this@filter) {
		if (filter(e)) yield(e)
	}
}

suspend fun <T> AsyncSequence<T>.chunks(count: Int) = asyncGenerate<List<T>> {
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

suspend fun <T> AsyncSequence<T>.toList(): List<T> = asyncFun {
	val out = arrayListOf<T>()
	for (e in this@toList) out += e
	out
}

inline suspend fun <T, TR> AsyncSequence<T>.fold(initial: TR, crossinline folder: (T, TR) -> TR): TR = asyncFun {
	var result: TR = initial
	for (e in this) result = folder(e, result)
	result
}

suspend fun AsyncSequence<Int>.sum(): Int = this.fold(0) { a, b -> a + b }


suspend fun <T> AsyncSequence<T>.isEmpty(): Boolean = asyncFun {
	var hasItems = false
	for (e in this@isEmpty) {
		hasItems = true
		break
	}
	hasItems
}

suspend fun <T> AsyncSequence<T>.isNotEmpty(): Boolean = asyncFun {
	var hasItems = false
	for (e in this@isNotEmpty) {
		hasItems = true
		break
	}
	!hasItems
}

suspend fun <T : Any?> AsyncSequence<T>.firstOrNull(): T? = asyncFun {
	var result: T? = null
	for (e in this@firstOrNull) {
		result = e
		break
	}
	result
}
