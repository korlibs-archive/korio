package com.soywiz.korio.stream

import com.soywiz.korio.util.toHexString
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncProduceConsumerByteBufferTest {
	@Test
	fun name() {
		val buf = SyncProduceConsumerByteBuffer()
		buf.produce(byteArrayOf(1))
		buf.produce(byteArrayOf(2, 3, 4))
		buf.produce(byteArrayOf(5, 6, 7, 8))
		assertEquals("010203040506", buf.consumeUntil(6).toHexString())
		assertEquals("0708", buf.consume(6).toHexString())
	}

	@Test
	fun name2() {
		val buf = SyncProduceConsumerByteBuffer()
		for (n in 1..8) buf.produce(byteArrayOf(n.toByte()))
		assertEquals("010203040506", buf.consumeUntil(6).toHexString())
		assertEquals("0708", buf.consume(6).toHexString())
	}

	@Test
	fun name3() {
		val buf = SyncProduceConsumerByteBuffer()
		buf.produce((1..8).map(Int::toByte).toByteArray())
		assertEquals("010203040506", buf.consumeUntil(6).toHexString())
		assertEquals("0708", buf.consume(6).toHexString())
	}

	@Test
	fun name4() {
		val buf = SyncProduceConsumerByteBuffer()
		buf.produce((1..8).map(Int::toByte).toByteArray())
		assertEquals("0102030405060708", buf.consumeUntil(20).toHexString())
		assertEquals("", buf.consume(6).toHexString())
	}
}