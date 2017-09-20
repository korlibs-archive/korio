package com.soywiz.korio.inject

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.expectException
import org.junit.Test
import kotlin.test.assertEquals


class AsyncInjectorTest {
	class Holder {
		var lastId = 0
		var log = ""
	}

	@Test
	fun testSimple() = syncTest {
		val inject = AsyncInjector()
		inject.mapTyped(10)
		assertEquals(10, inject.get<Int>())
	}

	@Test
	fun testSingleton() = syncTest {
		@Singleton
		class A(val holder: Holder) {
			val id: Int = holder.lastId++
		}

		val holder = Holder()
		val inject = AsyncInjector()
		inject.mapTyped(holder)
		val a0 = inject.get<A>()
		val a1 = inject.child().child().get<A>()
		assertEquals(0, a0.id)
		assertEquals(0, a1.id)
	}

	companion object {
		var lastId = 0
	}

	@Prototype
	class PrototypeA {
		val id: Int = lastId++
	}

	@Singleton
	class SingletonS {
		val id: Int = lastId++
	}

	@Prototype
	class SingletonA(val s: SingletonS) {
		val id: Int = lastId++
	}

	@Test
	fun testPrototype() = syncTest {
		lastId = 0
		val inject = AsyncInjector()
		val a0 = inject.get<PrototypeA>()
		val a1 = inject.child().child().get<PrototypeA>()
		assertEquals(0, a0.id)
		assertEquals(1, a1.id)
	}

	@Test
	fun testPrototypeSingleton() = syncTest {
		lastId = 0
		val inject = AsyncInjector()
		val a0 = inject.get<SingletonA>()
		val a1 = inject.child().child().get<SingletonA>()
		assertEquals(0, a0.s.id)
		assertEquals(0, a1.s.id)
		assertEquals(1, a0.id)
		assertEquals(2, a1.id)
	}

	@Test
	fun testAnnotation() = syncTest {
		annotation class Path(val path: String)

		@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
		@Prototype
		class B(
			val path: Path,
			val v: Int
		)

		@Singleton
		class A(
			@Path("mypath1") val b1: B,
			@Path("mypath2") val b2: B
		)

		val inject = AsyncInjector()
		inject.mapTyped(10)
		val a = inject.get<A>()
		assertEquals("mypath1", a.b1.path.path)
		assertEquals("mypath2", a.b2.path.path)
		assertEquals(10, a.b1.v)
		assertEquals(10, a.b2.v)
	}

	annotation class Path(val path: String)

	// e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: BitmapFontLoader
	//@AsyncFactoryClass(BitmapFontLoader::class)
	class BitmapFont(val path: String)

	class BitmapFontLoader(val path: Path) : AsyncFactory<BitmapFont> {
		override suspend fun create() = BitmapFont(path.path)
	}

	@Test
	fun testLoader() = syncTest {
		@Singleton
		class Demo(
			@Path("path/to/font") val font: BitmapFont
		)

		val inject = AsyncInjector()
		val demo = inject.get<Demo>()
		assertEquals("path/to/font", demo.font.path)
	}


	annotation class Path2A(val path1: String)
	annotation class Path2B(val path2: String)

	// e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: BitmapFontLoader
	//@AsyncFactoryClass(BitmapFontLoader2::class)
	class BitmapFont2(val path: String)

	class BitmapFontLoader2(
		@Optional val pathA: Path2A?,
		@Optional val pathB: Path2B?
	) : AsyncFactory<BitmapFont2> {
		override suspend fun create() = if (pathA != null) BitmapFont2(pathA.path1) else if (pathB != null) BitmapFont2(pathB.path2) else invalidOp
	}

	@Test
	fun testLoader2() = syncTest {
		@Singleton
		class Demo2(
			@Path2A("path/to/font/A") val fontA: BitmapFont2,
			@Path2B("path/to/font/B") val fontB: BitmapFont2
		)

		val inject = AsyncInjector()
		val demo = inject.get<Demo2>()
		assertEquals("path/to/font/A", demo.fontA.path)
		assertEquals("path/to/font/B", demo.fontB.path)
	}

	//@Inject lateinit var injector: AsyncInjector

	@Test
	fun testInjectAnnotation() = syncTest {
		val holder = Holder()

		open class Base : AsyncDependency {
			@Inject lateinit private var injector: AsyncInjector
			@Inject lateinit private var holder: Holder

			override suspend fun init() {
				holder.log += "Base.init<" + injector.get<Int>() + ">"
			}
		}

		@Singleton
		class Demo(
			val a: Int
		) : Base() {
			override suspend fun init() {
				super.init()
				holder.log += "Demo.init<$a>"
			}
		}

		val inject = AsyncInjector().mapTyped(holder)
		inject.mapTyped(10)
		val demo = inject.get<Demo>()
		assertEquals(10, demo.a)
	}

	@Test
	fun testSingletonInChilds() = syncTest {
		@Singleton
		class MySingleton {
			var a = 10
		}

		val injector = AsyncInjector()
		injector.child().get<MySingleton>().a = 20
		assertEquals(20, injector.get<MySingleton>().a)
	}

	@Test
	fun testNotMapped() = syncTest {
		expectException<AsyncInjector.NotMappedException> {
			data class Unmapped(val name: String)
			@Singleton
			class MySingleton(val unmapped: Unmapped)

			val injector = AsyncInjector()
			injector.child().get<MySingleton>()
		}
	}

	@Test
	fun testMap1() = syncTest {
		data class Mapped(val name: String)
		@Singleton
		class MySingleton(val mapped: Mapped)

		val injector = AsyncInjector()
		injector.child()
			.mapTyped<Mapped>(Mapped("hello"))
			.get<MySingleton>()
	}
}
