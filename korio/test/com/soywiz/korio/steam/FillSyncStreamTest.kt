package com.soywiz.korio.steam

import com.soywiz.korio.stream.FillSyncStream
import com.soywiz.korio.stream.readS16_be
import com.soywiz.korio.stream.readS16_le
import com.soywiz.korio.stream.readS8
import org.junit.Assert
import org.junit.Test

class FillSyncStreamTest {
	@Test
	fun name() {
		Assert.assertEquals(0, FillSyncStream(0).readS8())
		Assert.assertEquals(-1, FillSyncStream(0xFF).readS8())
		Assert.assertEquals(-1, FillSyncStream(0xFF).readS16_le())
		Assert.assertEquals(-1, FillSyncStream(0xFF).readS16_be())
	}
}