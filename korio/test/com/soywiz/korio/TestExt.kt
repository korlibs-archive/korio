package com.soywiz.korio

import org.junit.Assert

inline fun <reified T : Throwable> expectException(callback: () -> Unit) {
	try {
		callback()
		Assert.fail("Expected exception ${T::class.java.name} but nothing thrown")
	} catch (t: Throwable) {
		if (t is T) {
			Assert.assertTrue(true)
		} else {
			Assert.fail("Expected exception ${T::class.java.name} but found $t")
		}
	}
}