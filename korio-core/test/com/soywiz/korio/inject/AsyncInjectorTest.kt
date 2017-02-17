package com.soywiz.korio.inject

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.error.invalidOp
import org.junit.Assert
import org.junit.Test

class AsyncInjectorTest {
	@Test
	fun testSimple() = syncTest {
		val inject = AsyncInjector()
		inject.map(10)
		Assert.assertEquals(10, inject.get<Int>())
	}

	@Test
	fun testAnnotation() = syncTest {
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

	annotation class Path(val path: String)

	@AsyncFactoryClass(BitmapFontLoader::class) class BitmapFont(val path: String)

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
		Assert.assertEquals("path/to/font", demo.font.path)
	}


	annotation class Path2A(val path1: String)
	annotation class Path2B(val path2: String)

	@AsyncFactoryClass(BitmapFontLoader2::class) class BitmapFont2(val path: String)

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
		Assert.assertEquals("path/to/font/A", demo.fontA.path)
		Assert.assertEquals("path/to/font/B", demo.fontB.path)
	}

	//@Inject lateinit var injector: AsyncInjector

	@Test
	fun testInjectAnnotation() = syncTest {
		var log = ""

		open class Base : AsyncDependency {
			@Inject lateinit private var injector: AsyncInjector

			override suspend fun init() {
				log += "Base.init<" + injector.get<Int>() + ">"
			}
		}

		@Singleton
		class Demo(
			val a: java.lang.Integer
		) : Base() {
			override suspend fun init() {
				super.init()
				log += "Demo.init<$a>"
			}
		}

		val inject = AsyncInjector()
		inject.map(10)
		val demo = inject.get<Demo>()
		Assert.assertEquals(10, demo.a)
	}

}
