package com.soywiz.korio.async

class Signal<T> {
	internal val handlers = arrayListOf<(T) -> Unit>()

	fun add(handler: (T) -> Unit) {
		synchronized(handlers) { handlers += handler }
	}

	operator fun invoke(value: T) {
		for (handler in synchronized(handlers) { handlers.toList() }) {
			handler(value)
		}
		//while (handlers.isNotEmpty()) {
		//	val handler = handlers.remove()
		//	handler.invoke(value)
		//}
	}

	operator fun invoke(value: (T) -> Unit) = add(value)
}

operator fun Signal<Unit>.invoke() = invoke(Unit)
