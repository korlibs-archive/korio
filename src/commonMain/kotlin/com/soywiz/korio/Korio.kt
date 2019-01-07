package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.internal.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

fun Korio(entry: suspend CoroutineScope.() -> Unit) {
	//println("Korio[0]")
	asyncEntryPoint {
		//println("Korio[1]")
		entry(CoroutineScope(coroutineContext))
	}
}

object Korio {
	val VERSION = KORIO_VERSION
}
