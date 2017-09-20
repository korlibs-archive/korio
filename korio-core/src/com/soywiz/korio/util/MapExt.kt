package com.soywiz.korio.util

fun <K, V> Map<K, V>.flip(): Map<V, K> = this.map { Pair(it.value, it.key) }.toMap()

fun <K, V> Map<K, V>.toTreeMap(comparator: Comparator<K>): Map<K, V> {
	/*
	val tm = TreeMap<K, V>(comparator)
	tm.putAll(this)
	//return tm
	return tm
	*/
	TODO()
}

fun <V> Map<String, V>.toCaseInsensitiveTreeMap(): Map<String, V> {
	TODO()
}