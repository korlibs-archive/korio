package com.soywiz.korio.steam

import com.soywiz.korio.stream.FillSyncStream
import com.soywiz.korio.stream.readS16_be
import com.soywiz.korio.stream.readS16_le
import com.soywiz.korio.stream.readS8
import org.junit.Test
import kotlin.test.assertEquals

class FillSyncStreamTest {
	@Test
	fun name() {
		assertEquals(0, FillSyncStream(0).readS8())
		assertEquals(-1, FillSyncStream(0xFF).readS8())
		assertEquals(-1, FillSyncStream(0xFF).readS16_le())
		assertEquals(-1, FillSyncStream(0xFF).readS16_be())
	}
}