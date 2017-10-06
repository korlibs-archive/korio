package com.soywiz.korio.ext.db.cassandra

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.stream.AsyncProduceConsumerByteBuffer
import com.soywiz.korio.stream.writeBytes
import com.soywiz.korio.util.AsyncCloseable
import org.junit.Test
import kotlin.test.assertEquals

class CassandraTest {
	@Test
	fun name() = syncTest {
		val serverToClient = AsyncProduceConsumerByteBuffer()
		val clientToServer = AsyncProduceConsumerByteBuffer()

		val server = serverToClient

		serverToClient.writeBytes(Cassandra.Packet(
			version = 3,
			flags = 0,
			stream = 0,
			opcode = Cassandra.Opcodes.READY,
			payload = byteArrayOf()
		).toByteArray())

		val cassandra = Cassandra.create(
			reader = serverToClient,
			writer = clientToServer,
			close = AsyncCloseable.DUMMY,
			bufferSize = 1, debug = true
		)

		val requestPacket = Cassandra.Packet.read(clientToServer)

		assertEquals(
			Cassandra.Packet(
				version = 3, flags = 0, stream = 0, opcode = 1,
				payload = byteArrayOf(0, 1, 0, 11, 67, 81, 76, 95, 86, 69, 82, 83, 73, 79, 78, 0, 5, 51, 46, 48, 46, 48)
			),
			requestPacket
		)


		println("----------")

		//cassandra.query("SELECT 1;")
		//println("----------")
	}

	//@Test
	//@Ignore
	//fun integration() = syncTest {
	//	val cassandra = Cassandra.create(debug = true)
	//	val result = cassandra.query("SELECT 1;")
	//	println(result)
	//}
}