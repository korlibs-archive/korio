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

fun <T> Map<String, T>.toCaseInsensitiveTreeMap(): Map<String, T> {
	val res = CaseInsensitiveHashMap<T>()
	res.putAll(this)
	return res
}

class CaseInsensitiveHashMap<T>(
	private val mapOrig: HashMap<String, T> = HashMap<String, T>(),
	private val lcToOrig: HashMap<String, String> = HashMap<String, String>(),
	private val mapLC: HashMap<String, T> = HashMap<String, T>()
) : MutableMap<String, T> by mapOrig {
	override fun containsKey(key: String): Boolean = mapLC.containsKey(key.toLowerCase())

	override fun clear() {
		mapOrig.clear()
		mapLC.clear()
		lcToOrig.clear()
	}

	override fun get(key: String): T? = mapLC[key.toLowerCase()]

	override fun put(key: String, value: T): T? {
		remove(key)
		mapOrig.put(key, value)
		lcToOrig.put(key.toLowerCase(), key)
		return mapLC.put(key.toLowerCase(), value)
	}

	override fun putAll(from: Map<out String, T>) {
		for (v in from) put(v.key, v.value)
	}

	override fun remove(key: String): T? {
		val lkey = key.toLowerCase()
		val okey = lcToOrig[lkey]
		mapOrig.remove(okey)
		val res = mapLC.remove(lkey)
		lcToOrig.remove(lkey)
		return res
	}
}