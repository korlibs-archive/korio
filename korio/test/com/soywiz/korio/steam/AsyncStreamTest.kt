package com.soywiz.korio.steam

import com.soywiz.korio.async.sync
import com.soywiz.korio.stream.FillSyncStream
import com.soywiz.korio.stream.readU8
import com.soywiz.korio.stream.toAsync
import org.junit.Test

class AsyncStreamTest {
	@Test
	fun name() = sync {
		val mem = FillSyncStream(0).toAsync()
		println(mem.readU8())
	}
}