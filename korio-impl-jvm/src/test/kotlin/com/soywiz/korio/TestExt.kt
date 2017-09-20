package com.soywiz.korio

import kotlin.test.assertTrue
import kotlin.test.fail

inline fun <reified T : Throwable> expectException(callback: () -> Unit) {
	try {
		callback()
		fail("Expected exception ${T::class.java.name} but nothing thrown")
	} catch (t: Throwable) {
		if (t is T) {
			assertTrue(true)
		} else {

			fail("Expected exception ${T::class.java.name} but found $t")
		}
	}
}