package com.soywiz.korio.serialization.yaml

import org.junit.Assert
import org.junit.Test

class YamlTest {
	@Test
	fun name() {
		Assert.assertEquals(
			listOf(1, 2, 3),
			Yaml.read("""
			- 1
			- 2
			- 3
			""".trimIndent())
		)
	}

	@Test
	fun name2() {
		Assert.assertEquals(
			mapOf("hr" to 65, "avg" to 0.278, "rbi" to 147),
			Yaml.read("""
				hr:  65    # Home runs
				avg: 0.278 # Batting average
				rbi: 147   # Runs Batted In
			""".trimIndent())
		)
	}

	//@Test
	//fun name3() {
	//	Assert.assertEquals(
	//		mapOf("hr" to 65, "avg" to 0.278, "rbi" to 147),
	//		Yaml.read("""
	//			|-
	//			|	a: 1
	//			|-
	//			|	a: 2
	//		""".trimMargin())
	//	)
	//}
}