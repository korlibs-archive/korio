package com.soywiz.korio.util

import com.soywiz.korio.lang.Closeable

fun Iterable<Cancellable>.cancel(e: Throwable = com.soywiz.korio.CancellationException("")): Unit = run { for (c in this) c.cancel(e) }
fun Iterable<Cancellable>.cancellable() = Cancellable { this.cancel() }

fun Iterable<Closeable>.close() = run { for (c in this) c.close() }
fun Iterable<Closeable>.closeable() = Closeable { this.close() }

fun Closeable.cancellable() = Cancellable { this.close() }
fun Cancellable.closeable(e: () -> Throwable = { com.soywiz.korio.CancellationException("") }) = Closeable { this.cancel(e()) }