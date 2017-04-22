package com.soywiz.korio.util

import org.junit.Assert
import org.junit.Test

class ComputedTest {
	class Format(override var parent: Format? = null) : Computed.WithParent<Format> {
		var size: Int? = null

		val computedSize by Computed(Format::size) { 10 }
	}

	@Test
	fun name() {
		val f2 = Format()
		val f1 = Format(f2)
		Assert.assertEquals(10, f1.computedSize)
		f2.size = 12
		Assert.assertEquals(12, f1.computedSize)
		f1.size = 15
		Assert.assertEquals(15, f1.computedSize)
	}
}