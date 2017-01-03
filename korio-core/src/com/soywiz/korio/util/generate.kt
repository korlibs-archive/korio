package com.soywiz.korio.util

import kotlin.coroutines.Continuation
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.suspendCoroutine

// From: https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/generate.kt

@RestrictsSuspension
interface Generator<in T> {
	suspend fun yield(value: T)
}

fun <T> generate(block: suspend Generator<T>.() -> Unit): Iterable<T> = object : Iterable<T> {
	override fun iterator(): Iterator<T> {
		val iterator = GeneratorIterator<T>()
		iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
		return iterator
	}
}

private class GeneratorIterator<T>: AbstractIterator<T>(), Generator<T>, Continuation<Unit> {
	lateinit var nextStep: Continuation<Unit>

	// AbstractIterator implementation
	override fun computeNext() { nextStep.resume(Unit) }

	// Completion continuation implementation
	override fun resume(value: Unit) { done() }
	override fun resumeWithException(exception: Throwable) { throw exception }

	// Generator implementation
	override suspend fun yield(value: T) {
		setNext(value)
		return suspendCoroutine { c -> nextStep = c }
	}
}