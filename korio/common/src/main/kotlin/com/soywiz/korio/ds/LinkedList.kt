package com.soywiz.korio.ds

/*
class LinkedList<T> {
	var size: Int = 0; private set
	private val items = arrayListOf<T>()
	private val nextList = arrayListOf<Int>()
	private val prevList = arrayListOf<Int>()
	private val freeList = arrayListOf<Int>()
	private var first: Int = 0
}
*/

// @TODO: Reimplement like the previous one + implement Collection
class LinkedList<T>(private val list: ArrayList<T> = arrayListOf<T>()) : MutableCollection<T> by list {

	override val size: Int get() = list.size

	fun isNotEmpty(): Boolean = size != 0
	override fun isEmpty(): Boolean = size == 0

	fun addAll(items: Iterable<T>) = list.addAll(items)
	fun addFirst(item: T) = list.add(0, item)
	fun addLast(item: T) = list.add(item)
	fun removeFirst() = list.removeAt(0)
	fun removeLast() = list.removeAt(list.size - 1)

	val last: T get() = list.last()
}
