package com.soywiz.korio.ds

class LinkedList2<T : LinkedList2.Node<T>> : MutableIterable<T> {
	override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
		var current: T? = null
		var next: T? = synchronized(this@LinkedList2) { head }

		override fun hasNext(): Boolean = next != null

		override fun remove(): Unit = synchronized(this@LinkedList2) {
			if (current != null) this@LinkedList2.remove(current!!)
		}

		override fun next(): T = synchronized(this@LinkedList2) {
			val res = next
			next = next?.next
			current = res
			return@synchronized res!!
		}

	}

	private var _size: Int = 0
	private var head: T? = null
	private var tail: T? = null

	val size: Int get() = _size

	open class Node<T : Node<T>> {
		internal var list: LinkedList2<T>? = null
		internal var prev: T? = null
		internal var next: T? = null
	}

	fun remove(item: T): Unit = synchronized(this) {
		if (item.list == null) return@synchronized
		if (item.list != this) return@synchronized item.list!!.remove(item)
		item.prev?.next = item.next
		item.next?.prev = item.prev
		if (item == head) head = item.next
		if (item == tail) tail = item.prev
		item.list = null
		_size--
	}

	fun add(item: T): Unit = synchronized(this) {
		if (item.list != null) {
			item.list!!.remove(item)
		}
		if (head == null) head = item
		if (tail == null) {
			tail = item
		} else {
			tail!!.next = item
			item.prev = tail
			tail = item
		}
		item.list = this
		_size++
	}
}