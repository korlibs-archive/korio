package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.SyncProduceConsumerByteBuffer
import com.soywiz.korio.stream.toAsyncInputStream
import com.soywiz.korio.stream.toAsyncOutputStream
import com.soywiz.korio.stream.writeString
import com.soywiz.korio.util.AsyncCloseable
import org.junit.Assert.assertEquals
import org.junit.Test

class RedisTest {
	@Test
	fun name() = syncTest {
		val serverToClient = SyncProduceConsumerByteBuffer()
		val clientToServer = SyncProduceConsumerByteBuffer()

		val server = serverToClient.toAsyncOutputStream()

		server.writeString("+OK\r\n")
		server.writeString("\$5\r\nworld\r\n")
		server.writeString("\$5\r\nworld\r\n")

		val redis = Redis(reader = serverToClient.toAsyncInputStream(), writer = clientToServer.toAsyncOutputStream(), close = AsyncCloseable.DUMMY)
		//val redis = Redis()
		redis.set("hello", "world")
		assertEquals("world", redis.get("hello"))
		assertEquals("world", redis.echo("world"))
	}
}