package com.soywiz.korio.ds

class Stack<T>() {
	private val items = arrayListOf<T>()

	val size: Int get() = items.size

	constructor(vararg items: T) : this() {
		for (item in items) push(item)
	}

	fun push(v: T) {
		items.add(v)
	}

	fun pop(): T = items.removeAt(items.size - 1)
}

class Queue<T>() {
	private val items = LinkedList<T>()

	val size: Int get() = items.size

	constructor(vararg items: T) : this() {
		for (item in items) queue(item)
	}

	fun queue(v: T) {
		items.addLast(v)
	}

	fun dequeue(): T = items.removeFirst()
}