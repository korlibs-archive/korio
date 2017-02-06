package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.syncTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RedisTest {
	@Test
	fun name() = syncTest {
		val redis = Redis()
		redis.set("hello", "world")
		assertEquals("world", redis.get("hello"))
		assertEquals("world", redis.echo("world"))
	}
}