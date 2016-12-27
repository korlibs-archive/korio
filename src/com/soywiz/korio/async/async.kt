package com.soywiz.korio.async

import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

inline suspend fun <T> asyncFun(routine: suspend () -> T): T = suspendCoroutine<T> { routine.startCoroutine(it) }

val workerLazyPool by lazy { Executors.newCachedThreadPool() }

suspend fun <T> executeInWorker(task: () -> T): T = suspendCoroutine<T> { c ->
    workerLazyPool.execute {
        try {
            val result = task()
            c.resume(result)
        } catch (e: Throwable) {
            c.resumeWithException(e)
        }
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////

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

///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////

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

typealias CancelHandler = Signal<Unit>

interface Consumer<T> {
    suspend fun consume(): T
    suspend fun consumeWithCancelHandler(cancel: CancelHandler): T
}

interface Producer<T> {
    fun produce(v: T): Unit
}

class ProduceConsumer<T> : Consumer<T>, Producer<T> {
    val items = LinkedList<T>()
    val consumers = LinkedList<(T) -> Unit>()

    override fun produce(v: T) {
        items.addLast(v)
        flush()
    }

    private fun flush() {
        while (items.isNotEmpty() && consumers.isNotEmpty()) {
            val consumer = consumers.removeFirst()
            val item = items.removeFirst()
            consumer(item)
        }
    }

    suspend override fun consume(): T = suspendCoroutine { c ->
        consumers += { c.resume(it) }
        flush()
    }

    suspend override fun consumeWithCancelHandler(cancel: CancelHandler): T = suspendCoroutine { c ->
        val consumer: (T) -> Unit = { c.resume(it) }
        cancel {
            consumers -= consumer
            c.resumeWithException(CancellationException())
        }
        consumers += consumer
        flush()
    }

}

fun <T> asyncProducer(callback: suspend Producer<T>.() -> Unit): Consumer<T> {
    val p = ProduceConsumer<T>()

    callback.startCoroutine(p, completion = object : Continuation<Unit> {
        override fun resumeWithException(exception: Throwable) {
            exception.printStackTrace()
        }

        override fun resume(value: Unit) {
        }
    })
    return p
}

///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////

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
