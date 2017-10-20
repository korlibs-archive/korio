package com.soywiz.korio.inject

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.syncTestIgnoreJs
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.expectException
import kotlin.test.assertEquals


class AsyncInjectorTest {
	class Holder {
		var lastId = 0
		var log = ""
	}

	@kotlin.test.Test
	fun testSimple() = syncTest {
		val inject = AsyncInjector()
		inject.mapInstance(Int::class, 10)
		assertEquals(10, inject.get(Int::class))
	}

	@kotlin.test.Test
	fun testSingleton() = syncTest {
		@Singleton
		class A(val holder: Holder) {
			val id: Int = holder.lastId++
		}

		val holder = Holder()
		val inject = AsyncInjector()
		inject.mapSingleton(A::class) { A(get(Holder::class)) }
		inject.mapInstance(Holder::class, holder)
		val a0 = inject.get(A::class)
		val a1 = inject.child().child().get(A::class)
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
	class PrototypeB(val s: SingletonS) {
		val id: Int = lastId++
	}

	@kotlin.test.Test
	fun testPrototype() = syncTest {
		lastId = 0
		val inject = AsyncInjector()
		inject.mapPrototype(PrototypeA::class) { PrototypeA() }
		val a0 = inject.get(PrototypeA::class)
		val a1 = inject.child().child().get<PrototypeA>()
		assertEquals(0, a0.id)
		assertEquals(1, a1.id)
	}

	@kotlin.test.Test
	fun testPrototypeSingleton() = syncTest {
		lastId = 0
		val inject = AsyncInjector()
		inject.mapPrototype(PrototypeA::class) { PrototypeA() }
		inject.mapSingleton(SingletonS::class) { SingletonS() }
		inject.mapPrototype(PrototypeB::class) { PrototypeB(get(SingletonS::class)) }
		val a0 = inject.getOrNull(PrototypeB::class)
		val a1 = inject.child().child().getOrNull(PrototypeB::class)
		assertEquals(0, a0?.s?.id)
		assertEquals(0, a1?.s?.id)
		assertEquals(1, a0?.id)
		assertEquals(2, a1?.id)
	}

	//annotation class Path(val path: String)
	data class VPath(val path: String)

	fun AsyncInjector.mapPath(path: String) = mapInstance(VPath::class, VPath("path"))

	// e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: BitmapFontLoader
	//@AsyncFactoryClass(BitmapFontLoader::class)
	class BitmapFont(val path: String)

	class BitmapFontLoader(val path: VPath) : AsyncFactory<BitmapFont> {
		override suspend fun create() = BitmapFont(path.path)
	}

	@kotlin.test.Test
	fun testLoader() = syncTest {
		@Singleton
		class Demo(
			//@Path("path/to/font") val font: BitmapFont
		) : InjectorAsyncDependency {
			lateinit var font: BitmapFont

			suspend override fun init(injector: AsyncInjector) {
				font = injector.getWith(VPath("path/to/font"))
			}
		}

		val inject = AsyncInjector()
		inject.mapFactory(BitmapFont::class) { BitmapFontLoader(get()) }
		inject.mapSingleton(Demo::class) { Demo() }
		val demo = inject.get(Demo::class)
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
		override suspend fun create(): BitmapFont2 = when {
			pathA != null -> BitmapFont2(pathA.path1)
			pathB != null -> BitmapFont2(pathB.path2)
			else -> invalidOp("Boath pathA and pathB are null")
		}
	}

	//@kotlin.test.Test
	//fun testLoader2() = syncTest {
	//	@Singleton
	//	class Demo2 : InjectorAsyncDependency {
	//		lateinit var fontA: BitmapFont2
	//		lateinit var fontB: BitmapFont2
//
	//		suspend override fun init(injector: AsyncInjector) {
	//			fontA = injector.getPath("path/to/font/A")
	//			fontB = injector.getPath("path/to/font/B")
	//		}
	//	}
//
//
	//	val inject = AsyncInjector()
	//	inject.mapFactory { BitmapFontLoader2(getOrNull(), getOrNull()) }
	//	inject.mapSingleton { Demo2() }
	//	val demo = inject.get<Demo2>()
	//	assertEquals("path/to/font/A", demo.fontA.path)
	//	assertEquals("path/to/font/B", demo.fontB.path)
	//}

	//@Inject lateinit var injector: AsyncInjector

	@kotlin.test.Test
	// @TODO: Check why this fails on Kotlin.JS!
	fun testInjectAnnotation() = syncTestIgnoreJs {
		val holder = Holder()

		open class Base : InjectorAsyncDependency {
			lateinit var injector: AsyncInjector; private set
			lateinit var holder: Holder; private set

			override suspend fun init(injector: AsyncInjector) {
				this.injector = injector
				this.holder = injector.get(Holder::class)
				//holder.log += "Base.init<" + injector.get<Int>() + ">" // Not working with Kotlin.JS
				holder.log += "Base.init<" + injector.get(Int::class) + ">"
			}
		}

		@Singleton
		class Demo(
			val a: Int
		) : Base() {
			override suspend fun init(injector: AsyncInjector) {
				super.init(injector)
				holder.log += "Demo.init<$a>"
			}
		}

		val inject = AsyncInjector().mapInstance(Holder::class, holder)
		inject.mapInstance(Int::class, 10)
		// Not working with Kotlin.JS
		//inject.mapSingleton<Demo> { val demo = Demo(get()); demo.injector = get(); demo.holder = get(); demo } // Not working with Kotlin.JS
		//val demo = inject.get<Demo>() // Not working with Kotlin.JS
		inject.mapSingleton(Demo::class) { Demo(get(Int::class)) }
		val demo = inject.get(Demo::class)
		assertEquals(10, demo.a)
	}

	@kotlin.test.Test
	fun testSingletonInChilds() = syncTest {
		@Singleton
		class MySingleton {
			var a = 10
		}

		val injector = AsyncInjector()

		//injector.mapSingleton { MySingleton() }
		//injector.child().get<MySingleton>().a = 20
		//assertEquals(20, injector.get<MySingleton>().a)

		injector.mapSingleton(MySingleton::class) { MySingleton() }
		injector.child().get(MySingleton::class).a = 20
		assertEquals(20, injector.get(MySingleton::class).a)
	}

	@kotlin.test.Test
	fun testNotMapped() = syncTest {
		// @TODO: Not working with Kotlin.JS

		//expectException<AsyncInjector.NotMappedException> {
		//	data class Unmapped(val name: String)
		//	@Singleton
		//	class MySingleton(val unmapped: Unmapped)
//
		//	val injector = AsyncInjector()
		//	injector.mapSingleton { MySingleton(get()) }
		//	injector.child().get<MySingleton>()
		//}

		expectException<AsyncInjector.NotMappedException> {
			data class Unmapped(val name: String)
			@Singleton
			class MySingleton(val unmapped: Unmapped)

			val injector = AsyncInjector()
			injector.mapSingleton { MySingleton(get(Unmapped::class)) }
			injector.child().get(MySingleton::class)
		}
	}

	@kotlin.test.Test
	fun testMap1() = syncTest {
		data class Mapped(val name: String)
		@Singleton
		class MySingleton(val mapped: Mapped)

		val injector = AsyncInjector()
		injector.mapSingleton(MySingleton::class) { MySingleton(get()) }
		injector.child()
			.mapInstance(Mapped::class, Mapped("hello"))
			.get(MySingleton::class)
	}
}

private inline suspend fun <reified T: Any> AsyncInjector.getPath(path: String) = getWith<T>(AsyncInjectorTest.VPath(path))
