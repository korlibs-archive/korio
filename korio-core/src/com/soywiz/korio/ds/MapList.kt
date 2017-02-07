package com.soywiz.korio.ds

import java.util.*

class MapList<K, V>() : Iterable<Pair<K, List<V>>> {
	override fun iterator(): Iterator<Pair<K, List<V>>> = map.entries.map { it.key to it.value }.iterator()

	fun flatMapIterator(): Iterator<Pair<K, V>> = map.entries.flatMap { item -> item.value.map { item.key to it } }.iterator()

	constructor(items: List<Pair<K, V>>) : this() {
		for ((k, v) in items) append(k, v)
	}

	constructor(items: Map<K, V>) : this() {
		for ((k, v) in items) append(k, v)
	}

	constructor(items: MapList<K, V>) : this() {
		for ((k, values) in items) for (v in values) append(k, v)
	}

	val map = hashMapOf<K, ArrayList<V>>()

	fun append(key: K, value: V): MapList<K, V> {
		map.getOrPut(key) { arrayListOf() }
		map[key]!! += value
		return this
	}

	fun replace(key: K, value: V): MapList<K, V> {
		map.remove(key)
		append(key, value)
		return this
	}

	fun appendAll(vararg items: Pair<K, V>): MapList<K, V> = this.apply { for ((k, v) in items) append(k, v) }
	fun replaceAll(vararg items: Pair<K, V>): MapList<K, V> = this.apply { for ((k, v) in items) replace(k, v) }

	fun get(key: K) = map[key]

	fun getFirst(key: K) = map[key]?.firstOrNull()


}
