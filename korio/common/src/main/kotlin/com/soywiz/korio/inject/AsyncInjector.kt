package com.soywiz.korio.inject

import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.util.Extra
import kotlin.reflect.KClass

//import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Prototype

@Target(AnnotationTarget.CLASS)
annotation class Singleton

//@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
//@Target(AnnotationTarget.)
@Deprecated("Do not use Inject but injector.get() with a lateinit")
annotation class Inject

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class Optional

interface AsyncObjectProvider<T> {
	suspend fun get(injector: AsyncInjector): T
}

class PrototypeAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> T) : AsyncObjectProvider<T> {
	override suspend fun get(injector: AsyncInjector): T = injector.created(generator(injector))
	override fun toString(): String = "PrototypeAsyncObjectProvider()"
}

class FactoryAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> AsyncFactory<T>) : AsyncObjectProvider<T> {
	override suspend fun get(injector: AsyncInjector): T = injector.created(generator(injector).create())
	override fun toString(): String = "FactoryAsyncObjectProvider()"
}

class SingletonAsyncObjectProvider<T>(val generator: suspend AsyncInjector.() -> T) : AsyncObjectProvider<T> {
	var value: T? = null
	override suspend fun get(injector: AsyncInjector): T {
		if (value == null) value = injector.created(generator(injector))
		return value!!
	}
	override fun toString(): String = "SingletonAsyncObjectProvider($value)"
}

class InstanceAsyncObjectProvider<T>(val instance: T) : AsyncObjectProvider<T> {
	override suspend fun get(injector: AsyncInjector): T = instance
	override fun toString(): String = "InstanceAsyncObjectProvider($instance)"
}

class AsyncInjector(val parent: AsyncInjector? = null, val level: Int = 0) : Extra by Extra.Mixin() {
	@Deprecated("Temporally incompatible with Kotlin.JS")
	suspend inline fun <reified T : Any> getWith(vararg instances: Any): T = getWith(T::class, *instances)

	@Deprecated("Temporally incompatible with Kotlin.JS")
	suspend inline fun <reified T : Any> get(): T = get<T>(T::class)

	@Deprecated("Temporally incompatible with Kotlin.JS")
	suspend inline fun <reified T : Any> getOrNull(): T? = getOrNull<T>(T::class)

	@Deprecated("Temporally incompatible with Kotlin.JS")
	inline fun <reified T : Any> mapInstance(instance: T): AsyncInjector = mapInstance(T::class, instance)

	@Deprecated("Temporally incompatible with Kotlin.JS")
	inline fun <reified T : Any> mapFactory(noinline gen: suspend AsyncInjector.() -> AsyncFactory<T>) = mapFactory(T::class, gen)

	@Deprecated("Temporally incompatible with Kotlin.JS")
	inline fun <reified T : Any> mapSingleton(noinline gen: suspend AsyncInjector.() -> T) = mapSingleton(T::class, gen)

	@Deprecated("Temporally incompatible with Kotlin.JS")
	inline fun <reified T : Any> mapPrototype(noinline gen: suspend AsyncInjector.() -> T) = mapPrototype(T::class, gen)

	var fallbackProvider: (suspend (clazz: kotlin.reflect.KClass<*>, ctx: RequestContext) -> AsyncObjectProvider<*>)? = null
	val providersByClass = lmapOf<kotlin.reflect.KClass<*>, AsyncObjectProvider<*>>()

	val root: AsyncInjector = parent?.root ?: this
	val nearestFallbackProvider get() = fallbackProvider ?: parent?.fallbackProvider

	fun child() = AsyncInjector(this, level + 1)

	suspend fun <T : Any> getWith(clazz: KClass<T>, vararg instances: Any): T {
		val c = child()
		for (i in instances) {
			c.mapInstance(i::class as KClass<Any>, i)
		}
		return c.get(clazz)
	}

	fun dump() {
		println("$this")
		for ((k, v) in providersByClass) {
			println("- $k: $v")
		}
		parent?.dump()
	}

	fun <T : Any> mapInstance(clazz: KClass<T>, instance: T): AsyncInjector = this.apply {
		providersByClass[clazz] = InstanceAsyncObjectProvider<T>(instance as T)
	}

	fun <T : Any> mapFactory(clazz: KClass<T>, gen: suspend AsyncInjector.() -> AsyncFactory<T>): AsyncInjector = this.apply {
		providersByClass[clazz] = FactoryAsyncObjectProvider<T>(gen)
	}

	fun <T : Any> mapSingleton(clazz: KClass<T>, gen: suspend AsyncInjector.() -> T): AsyncInjector = this.apply {
		providersByClass[clazz] = SingletonAsyncObjectProvider<T>(gen)
	}

	fun <T : Any> mapPrototype(clazz: KClass<T>, gen: suspend AsyncInjector.() -> T): AsyncInjector = this.apply {
		providersByClass[clazz] = PrototypeAsyncObjectProvider<T>(gen)
	}

	init {
		mapInstance(this)
	}

	data class RequestContext(val initialClazz: KClass<*>)

	suspend fun <T : Any> getProviderOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): AsyncObjectProvider<T>? = (
		providersByClass[clazz]
			?: parent?.getProviderOrNull<T>(clazz, ctx)
			?: nearestFallbackProvider?.invoke(clazz, ctx)
		) as? AsyncObjectProvider<T>?

	suspend fun <T : Any> getProvider(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): AsyncObjectProvider<T> =
		getProviderOrNull<T>(clazz, ctx) ?:
			throw AsyncInjector.NotMappedException(
				clazz, ctx.initialClazz, ctx, "Class '$clazz' doesn't have constructors $ctx"
			)

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any> getOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
		return getProviderOrNull<T>(clazz, ctx)?.get(this)
	}

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any> get(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T {
		return getProvider<T>(clazz, ctx).get(this)
	}

	suspend fun <T : Any> has(clazz: KClass<T>): Boolean = getProviderOrNull<T>(clazz) != null

	class NotMappedException(
		val clazz: KClass<*>,
		val requestedByClass: KClass<*>,
		val ctx: RequestContext,
		val msg: String = "Not mapped $clazz requested by $requestedByClass in $ctx"
	) : RuntimeException(msg)

	override fun toString(): String = "AsyncInjector(level=$level)"

	suspend internal fun <T> created(instance: T): T {
		if (instance is AsyncDependency) instance.init()
		if (instance is InjectorAsyncDependency) instance.init(this)
		return instance
	}
}

interface AsyncFactory<T> {
	suspend fun create(): T
}

interface InjectedHandler {
	suspend fun injectedInto(instance: Any): Unit
}

annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)
//annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)
//annotation class AsyncFactoryClass<T : Any>(val clazz: KClass<AsyncFactory<T>>)
//annotation class AsyncFactoryClass<T>(val clazz: kotlin.reflect.KClass<out AsyncFactory<*>>)

interface AsyncDependency {
	suspend fun init(): Unit
}

interface InjectorAsyncDependency {
	suspend fun init(injector: AsyncInjector): Unit
}
