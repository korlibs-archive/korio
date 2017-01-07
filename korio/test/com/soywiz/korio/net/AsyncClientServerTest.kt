package com.soywiz.korio.net

import com.soywiz.korio.async.go
import com.soywiz.korio.async.spawn
import com.soywiz.korio.async.sync
import com.soywiz.korio.stream.readString
import com.soywiz.korio.stream.writeString
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue

class AsyncClientServerTest {
	@Test
	fun testClientServer() = sync {
		val server = AsyncServer(port = 0)
		val events = ConcurrentLinkedQueue<String>()

		val client1 = spawn {
			val client = AsyncClient.createAndConnect("127.0.0.1", server.port)
			val str = client.readString(5)
			events += "[C] CLIENT1: $str"
			client.writeString("THIS IS GREAT!")
		}

		for (client in server.listen()) {
			client.writeString("HELLO")
			events += "[S] CLIENT: " + client.readString(100)
			break
		}

		client1.await()

		Assert.assertEquals(
			"[[C] CLIENT1: HELLO, [S] CLIENT: THIS IS GREAT!]",
			events.toList().toString()
		)
	}
}