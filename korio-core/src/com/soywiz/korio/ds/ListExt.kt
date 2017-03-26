package com.soywiz.korio.ds

fun <T> List<T>.getCyclic(index: Int) = this[index % this.size]