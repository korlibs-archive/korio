package com.soywiz.korio.ds

import com.soywiz.korio.error.*

class SubListGeneric<T>(val base: List<T>, val start: Int, val end: Int) : List<T> {
	init {
		if (start !in 0..base.size) throw OutOfBoundsException(start)
		if (end !in 0..base.size) throw OutOfBoundsException(start)
	}

	override val size: Int get() = end - start

	private fun Int.translateIndex(): Int {
		if (this !in 0 until size) throw OutOfBoundsException(this)
		return start + this
	}

	override fun contains(element: T): Boolean = (start until end).any { base[it] == element }

	override fun containsAll(elements: Collection<T>): Boolean {
		val elementsSet = if (elements is Set) elements else elements.toSet()
		return (start until end).any { base[it] in elementsSet }
	}

	override fun get(index: Int): T = base[index.translateIndex()]

	override fun indexOf(element: T): Int {
		val res = (start until end).indexOfFirst { this == element }
		return if (res < 0) res else start + res
	}

	override fun lastIndexOf(element: T): Int {
		val res = ((end - 1) downTo start).indexOfFirst { this == element }
		return if (res < 0) res else start + res
	}

	override fun isEmpty(): Boolean = size == 0
	override fun iterator(): Iterator<T> = listIterator(0)
	override fun listIterator(): ListIterator<T> = listIterator(0)
	override fun listIterator(index: Int): ListIterator<T> = GenericListIterator(this, index)
	override fun subList(fromIndex: Int, toIndex: Int): List<T> = SubListGeneric(this, fromIndex, toIndex)
}

class GenericListIterator<T>(val list: List<T>, val iindex: Int) : ListIterator<T> {
	init {
		if (iindex !in 0 until list.size) throw OutOfBoundsException(iindex)
	}

	private var index = iindex

	override fun hasNext(): Boolean = index < list.size

	override fun next(): T {
		if (!hasNext()) throw NoSuchElementException()
		return list.get(index++)
	}

	override fun hasPrevious(): Boolean = index > 0

	override fun nextIndex(): Int = index

	override fun previous(): T {
		if (!hasPrevious()) throw NoSuchElementException()
		return list.get(--index)
	}

	override fun previousIndex(): Int = index - 1
}