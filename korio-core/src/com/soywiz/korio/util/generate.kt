package com.soywiz.korio.util

import com.soywiz.korio.coroutine.*

// From: https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/generate.kt

@RestrictsSuspension
interface Generator<in T> {
	suspend fun yield(value: T)
}

fun <T> generate(block: suspend Generator<T>.() -> Unit): Iterable<T> = object : Iterable<T> {
	override fun iterator(): Iterator<T> {
		val iterator = GeneratorIterator<T>()
		iterator.nextStep = block.korioCreateCoroutine(receiver = iterator, completion = iterator)
		return iterator
	}
}

private class GeneratorIterator<T> : AbstractIterator<T>(), Generator<T>, Continuation<Unit> {
	override val context: CoroutineContext = EmptyCoroutineContext

	lateinit var nextStep: Continuation<Unit>

	// AbstractIterator implementation
	override fun computeNext() {
		nextStep.resume(Unit)
	}

	// Completion continuation implementation
	override fun resume(value: Unit) {
		done()
	}

	override fun resumeWithException(exception: Throwable) {
		throw exception
	}

	// Generator implementation
	override suspend fun yield(value: T) {
		setNext(value)
		return korioSuspendCoroutine { c -> nextStep = c }
	}
}