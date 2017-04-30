@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.ds.LinkedList2
import java.io.Closeable

class Signal<T>(val onRegister: () -> Unit = {}) { //: AsyncSequence<T> {
	inner class Node(val once: Boolean, val item: (T) -> Unit) : LinkedList2.Node<Node>(), Closeable {
		override fun close() {
			handlers.remove(this)
		}
	}

	private var handlers = LinkedList2<Node>()

	val listenerCount: Int get() = handlers.size

	fun once(handler: (T) -> Unit): Closeable = _add(true, handler)
	fun add(handler: (T) -> Unit): Closeable = _add(false, handler)

	private fun _add(once: Boolean, handler: (T) -> Unit): Closeable {
		onRegister()
		val node = Node(once, handler)
		handlers.add(node)
		return node
	}

	operator fun invoke(value: T) {
		val it = handlers.iterator()
		while (it.hasNext()) {
			val node = it.next()
			if (node.once) it.remove()
			node.item(value)
		}
	}

	operator fun invoke(handler: (T) -> Unit): Closeable = add(handler)

	suspend fun listen(): AsyncSequence<T> = asyncGenerate {
		while (true) {
			yield(waitOne())
		}
	}


//override fun iterator(): AsyncIterator<T> = asyncGenerate {
//	while (true) {
//		yield(waitOne())
//	}
//}.iterator()
}

//class AsyncSignal<T>(context: CoroutineContext) {

//}

fun <TI, TO> Signal<TI>.mapSignal(transform: (TI) -> TO): Signal<TO> {
	val out = Signal<TO>()
	this.add { out(transform(it)) }
	return out
}

operator fun Signal<Unit>.invoke() = invoke(Unit)

suspend fun <T> Signal<T>.waitOne(): T = suspendCancellableCoroutine { c ->
	var close: Closeable? = null
	close = once {
		close?.close()
		c.resume(it)
	}
	c.onCancel {
		close?.close()
	}
}
