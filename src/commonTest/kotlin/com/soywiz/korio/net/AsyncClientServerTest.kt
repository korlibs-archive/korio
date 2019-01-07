package com.soywiz.korio.net

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.*
import kotlin.test.*

class AsyncClientServerTest {
	companion object {
		val UUIDLength = 36
	}

	@Ignore
	@Test
	fun testClientServer() = suspendTest {
		val server = AsyncServer(port = 0)

		val clientsCount = 2000
		//val clientsCount = 10
		var counter = 0
		val correctEchoes = Deque<Boolean>()

		val clients = (0 until clientsCount).map { clientId ->
			asyncImmediately(coroutineContext) {
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

		clients.awaitAll()

		assertEquals(clientsCount, counter)
		assertEquals(clientsCount, correctEchoes.size)
		assertTrue(correctEchoes.all { it })
	}
}
