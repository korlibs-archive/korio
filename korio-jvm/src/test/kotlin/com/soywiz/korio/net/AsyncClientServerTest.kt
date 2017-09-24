package com.soywiz.korio.net

import com.soywiz.korio.async.await
import com.soywiz.korio.async.spawn
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.take
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.writeString
import org.junit.Ignore
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsyncClientServerTest {
	companion object {
		val UUIDLength = 36
	}

	@Ignore
	@Test
	fun testClientServer() = syncTest {
		val server = AsyncServer(port = 0)

		val clientsCount = 2000
		//val clientsCount = 10
		var counter = 0
		val correctEchoes = LinkedList<Boolean>()

		val clients = (0 until clientsCount).map { clientId ->
			spawn(coroutineContext) {
				try {
					val client = AsyncClient.createAndConnect("127.0.0.1", server.port)

					val msg = UUID.randomUUID().toString()
					client.writeString(msg)
					val echo = client.readString(UUIDLength)

					correctEchoes.add(msg == echo)
				} catch (e: Throwable) {
					println("Client-$clientId failed")
					e.printStackTrace()

				}
			}
		}


		for (client in server.listen().take(clientsCount)) {
			val msg = client.readString(UUIDLength)
			client.writeString(msg)
			counter++
		}

		clients.await()

		assertEquals(clientsCount, counter)
		assertEquals(clientsCount, correctEchoes.size)
		assertTrue(correctEchoes.all { it })
	}
}
