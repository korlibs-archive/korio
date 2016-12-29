package com.soywiz.korio.async

class Signal<T> {
	internal val handlers = arrayListOf<(T) -> Unit>()

	fun add(handler: (T) -> Unit) {
		handlers += handler
	}

	operator fun invoke(value: T) {
		for (handler in handlers) handler.invoke(value)
	}

	operator fun invoke(value: (T) -> Unit) = add(value)
}

operator fun Signal<Unit>.invoke() = invoke(Unit)
