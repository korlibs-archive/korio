package com.soywiz.korio.async.tween

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.suspendCancellableCoroutine
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.clamp
import kotlin.reflect.KMutableProperty1

suspend fun <T> T.tween(vararg vs: VX<T, *>, time: Int, easing: Easing = Easing.LINEAR, callback: (Double) -> Unit = { }) = suspendCancellableCoroutine<Unit> { c ->
	val vs2 = vs.map { it.v2(this@tween) }
	val start = EventLoop.time
	fun step() {
		val current = EventLoop.time
		val elapsed = current - start
		val ratio = (elapsed.toDouble() / time.toDouble()).clamp(0.0, 1.0)
		val fratio = easing(ratio)
		for (v in vs2) {
			v.set(this@tween, fratio)
		}
		callback(fratio)
		if (ratio >= 1.0) {
			c.resume(Unit)
		} else {
			EventLoop.requestAnimationFrame { step() }
		}
	}
	step()
}

fun interpolate(v0: Int, v1: Int, step: Double): Int = (v0 * (1 - step) + v1 * step).toInt()
fun interpolate(v0: Long, v1: Long, step: Double): Long = (v0 * (1 - step) + v1 * step).toLong()
fun interpolate(v0: Double, v1: Double, step: Double): Double = v0 * (1 - step) + v1 * step

fun <T> interpolate(min: T, max: T, ratio: Double): Any = when (min) {
	is Int -> interpolate(min, max as Int, ratio)
	is Long -> interpolate(min, max as Long, ratio)
	is Double -> interpolate(min, max as Double, ratio)
	else -> invalidOp
}

interface VX<T, V> {
	fun v2(obj: T): V2<T, V>
}

class V1<T, V> internal constructor(val key: KMutableProperty1<T, V>, val value: V, unit: Unit) : VX<T, V> {
	override fun v2(obj: T): V2<T, V> = V2(key, key.get(obj), value, Unit)

	operator fun rangeTo(that: V) = V2(key, value, that, Unit)
}

@Suppress("UNCHECKED_CAST")
class V2<T, V> internal constructor(val key: KMutableProperty1<T, V>, val initial: V, val end: V, unit: Unit) : VX<T, V> {
	fun set(obj: T, ratio: Double) = key.set(obj, interpolate(initial as Any, end as Any, ratio) as V)
	override fun v2(obj: T): V2<T, V> = this
}

operator fun <T, V> KMutableProperty1<T, V>.rangeTo(that: V) = V1(this, that, Unit)
operator fun <T, V : Comparable<V>> KMutableProperty1<T, V>.rangeTo(that: ClosedRange<V>) = V2(this, that.start, that.endInclusive, Unit)
operator fun <T, V> KMutableProperty1<T, V>.rangeTo(that: Pair<V, V>) = V2(this, that.first, that.second, Unit)
