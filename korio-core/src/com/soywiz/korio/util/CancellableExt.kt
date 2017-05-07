package com.soywiz.korio.util

fun Iterable<Cancellable>.cancel(): Unit {
	for (c in this) c.cancel()
}

fun Iterable<Cancellable>.cancellable() = Cancellable { this.cancel() }