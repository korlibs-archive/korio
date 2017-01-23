package com.soywiz.korio.async.tween

import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import org.junit.Assert
import org.junit.Test

class TweenTest {
	@Test
	fun name() = sync(EventLoopTest()) {
		val result = arrayListOf<Any>()

		class Demo(var a: Int = -10)

		val demo = Demo()
		demo.tween(Demo::a..+10, time = 100, easing = Easing.LINEAR) {
			result += "[" + demo.a + ":" + it + "]"
		}
		result += "---"
		demo.tween(Demo::a..-100..+100, time = 100, easing = Easing.LINEAR) {
			result += "[" + demo.a + ":" + it + "]"
		}
		result += "---"
		Assert.assertEquals(
			"[-10:0.0],[-6:0.2],[-2:0.4],[2:0.6],[6:0.8],[10:1.0],---,[-100:0.0],[-60:0.2],[-20:0.4],[20:0.6],[60:0.8],[100:1.0],---",
			result.joinToString(",")
		)
	}
}