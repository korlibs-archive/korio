package com.soywiz.korio.util

import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.RestrictsSuspension
import com.soywiz.korio.coroutine.getCoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.createCoroutine
import kotlin.coroutines.experimental.suspendCoroutine

// From: https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/generate.kt

@RestrictsSuspension
interface Generator<in T> {
	suspend fun yield(value: T)
}

suspend fun <T> generateSync(block: suspend Generator<T>.() -> Unit): Iterable<T> {
	val cc = getCoroutineContext()
	return object : Iterable<T> {
		override fun iterator(): Iterator<T> {
			val iterator = GeneratorIterator<T>(cc)
			iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
			return iterator
		}
	}
}

fun <T> generate(block: suspend Generator<T>.() -> Unit): Iterable<T> {
	return object : Iterable<T> {
		override fun iterator(): Iterator<T> {
			val iterator = GeneratorIterator<T>(EmptyCoroutineContext)
			iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
			return iterator
		}
	}
}

private class GeneratorIterator<T>(override val context: CoroutineContext) : AbstractIterator<T>(), Generator<T>, Continuation<Unit> {
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
		return suspendCoroutine { c -> nextStep = c }
	}
}