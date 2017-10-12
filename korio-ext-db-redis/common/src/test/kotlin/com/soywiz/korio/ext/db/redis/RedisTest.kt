package com.soywiz.korio.ext.db.redis

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.SyncProduceConsumerByteBuffer
import com.soywiz.korio.stream.toAsyncInputStream
import com.soywiz.korio.stream.toAsyncOutputStream
import com.soywiz.korio.stream.writeString
import com.soywiz.korio.util.AsyncCloseable
import org.junit.Test
import kotlin.test.assertEquals

class RedisTest {
	@Test
	fun name() = syncTest {
		val serverToClient = SyncProduceConsumerByteBuffer()
		val clientToServer = SyncProduceConsumerByteBuffer()

		val server = serverToClient.toAsyncOutputStream()

		for (n in 0 until 10) {
			server.writeString("+OK\r\n\$5\r\nworld\r\n\$5\r\nworld\r\n")

			val redis = Redis.Client(reader = serverToClient.toAsyncInputStream(), writer = clientToServer.toAsyncOutputStream(), closeable = AsyncCloseable.DUMMY, bufferSize = 1)
			//val redis = Redis()
			redis.set("hello", "world")
			assertEquals("world", redis.get("hello"))
			assertEquals("world", redis.echo("world"))
		}
	}
}