package com.soywiz.korio.async

import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.withCoroutineContext
import com.soywiz.korio.ds.LinkedList2
import com.soywiz.korio.lang.Closeable

class AsyncSignal<T>(val onRegister: () -> Unit = {}) { //: AsyncSequence<T> {
	inner class Node(val once: Boolean, val item: suspend (T) -> Unit) : LinkedList2.Node<Node>(), Closeable {
		override fun close() {
			handlers.remove(this)
		}
	}

	private var handlers = LinkedList2<Node>()

	val listenerCount: Int get() = handlers.size

	fun once(handler: suspend (T) -> Unit): Closeable = _add(true, handler)
	fun add(handler: suspend (T) -> Unit): Closeable = _add(false, handler)

	private fun _add(once: Boolean, handler: suspend (T) -> Unit): Closeable {
		onRegister()
		val node = Node(once, handler)
		handlers.add(node)
		return node
	}

	suspend operator fun invoke(value: T) {
		val it = handlers.iterator()
		while (it.hasNext()) {
			val node = it.next()
			if (node.once) it.remove()
			node.item(value)
		}
	}

	operator fun invoke(handler: suspend (T) -> Unit): Closeable = add(handler)

	suspend fun listen(): AsyncSequence<T> = asyncGenerate {
		while (true) {
			yield(waitOne())
		}
	}
}

fun <TI, TO> AsyncSignal<TI>.mapSignal(transform: (TI) -> TO): AsyncSignal<TO> {
	val out = AsyncSignal<TO>()
	this.add { out(transform(it)) }
	return out
}

suspend operator fun AsyncSignal<Unit>.invoke() = invoke(Unit)

suspend fun <T> AsyncSignal<T>.waitOne(): T = suspendCancellableCoroutine { c ->
	var close: Closeable? = null
	close = once {
		close?.close()
		c.resume(it)
	}
	c.onCancel {
		close?.close()
	}
}


suspend fun <T> Signal<T>.addSuspend(handler: suspend (T) -> Unit): Closeable = withCoroutineContext {
	this@addSuspend { value ->
		async {
			handler(value)
		}
	}
}

fun <T> Signal<T>.addSuspend(context: CoroutineContext, handler: suspend (T) -> Unit): Closeable = this@addSuspend { value ->
	context.async {
		handler(value)
	}
}
