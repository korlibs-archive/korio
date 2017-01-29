package com.soywiz.korio.inject

import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync
import org.junit.Assert
import org.junit.Test

class AsyncInjectorTest {
	@Test
	fun name() = sync(EventLoopTest()) {
		val inject = AsyncInjector()
		inject.map(10)
		Assert.assertEquals(10, inject.get<Int>())
	}

	@Test
	fun name2() = sync(EventLoopTest()) {
		annotation class Path(val path: String)

		@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
		@Prototype class B(
			val path: Path,
		    val v: java.lang.Integer
		)

		@Singleton class A(
			@Path("mypath1") val b1: B,
			@Path("mypath2") val b2: B
		)

		val inject = AsyncInjector()
		inject.map(10)
		val a = inject.get<A>()
		Assert.assertEquals("mypath1", a.b1.path.path)
		Assert.assertEquals("mypath2", a.b2.path.path)
		Assert.assertEquals(10, a.b1.v)
		Assert.assertEquals(10, a.b2.v)
	}
}