package com.soywiz.korio.inject

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.Extra
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Prototype

@Target(AnnotationTarget.CLASS)
annotation class Singleton

//@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD)
//@Target(AnnotationTarget.)
annotation class Inject

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class Optional

abstract class AsyncObjectProvider {
	enum class Type { Singleton, Prototype, AsyncFactoryClass, None }

	suspend fun hasType(clazz: KClass<*>): Boolean = getType(clazz) != Type.None
	abstract suspend fun getType(clazz: KClass<*>): Type
	abstract suspend fun <T : Any> createInstance(injector: AsyncInjector, clazz: KClass<T>): T
}

class ManualAsyncObjectProvider : AsyncObjectProvider() {
	val types = hashMapOf<KClass<*>, Type>()
	val generators = hashMapOf<KClass<*>, suspend () -> Any?>()

	override suspend fun getType(clazz: KClass<*>): Type = types[clazz] ?: Type.None

	override suspend fun <T : Any> createInstance(injector: AsyncInjector, clazz: KClass<T>): T =
		generators[clazz]?.invoke() as? T? ?: invalidOp("Can't generate $clazz")
}

class AsyncInjector(val parent: AsyncInjector? = null, val provider: AsyncObjectProvider = ManualAsyncObjectProvider(), val level: Int = 0) : Extra by Extra.Mixin() {
	private val instancesByClass = hashMapOf<KClass<*>, Any?>()

	data class Item<T : Any>(val isSingleton: Boolean, val generate: suspend (clazz: KClass<T>) -> T)

	var fallbackProvider: AsyncObjectProvider? = null

	val actualFallbackProvider: AsyncObjectProvider? get() = fallbackProvider ?: parent?.actualFallbackProvider

	fun child() = AsyncInjector(this, provider, level + 1)

	val root: AsyncInjector = parent?.root ?: this

	suspend inline fun <reified T : Any> get() = get(T::class)

	inline fun <reified T : Any> mapTyped(instance: T): AsyncInjector = map(instance, T::class)
	fun <T : Any> map(instance: T, clazz: KClass<T>): AsyncInjector = this.apply { instancesByClass[clazz] = instance as Any }

	init {
		mapTyped<AsyncInjector>(this)
	}

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any> get(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T = getOrNull(clazz, ctx) ?: create(clazz, ctx) ?: invalidOp("Class '$clazz' doesn't have constructors $ctx")

	data class RequestContext(val initialClazz: KClass<*>)

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any> getOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
		return when (provider.getType(clazz)) {
			AsyncObjectProvider.Type.Prototype -> {
				val instance = create(clazz, ctx)
				instancesByClass[clazz] = instance
				instance
			}
			AsyncObjectProvider.Type.Singleton -> {
				val root = root
				//val root = this
				if (!has(clazz)) {
					val instance = create(clazz, ctx) ?: return null
					root.instancesByClass[clazz] = instance
					instance
				} else {
					(instancesByClass[clazz] ?: parent?.getOrNull(clazz, ctx)) as T?
				}
			}
			AsyncObjectProvider.Type.AsyncFactoryClass -> {
				create(clazz, ctx)
			}
			else -> {
				(instancesByClass[clazz] ?: parent?.getOrNull(clazz, ctx)) as T?
			}
		}
	}

	fun has(clazz: KClass<*>): Boolean = instancesByClass.containsKey(clazz) || parent?.has(clazz) ?: false

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any> create(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
		val obj = if (provider.hasType(clazz)) {
			provider.createInstance(this, clazz)
		} else {
			actualFallbackProvider?.createInstance(this, clazz)
		}
		if (obj is AsyncDependency) {
			obj.init()
		}
		return obj
	}

	class NotMappedException(val clazz: KClass<*>, val requestedByClass: KClass<*>, val ctx: RequestContext) : RuntimeException("Not mapped ${clazz} requested by ${requestedByClass} in $ctx")

	override fun toString(): String = "AsyncInjector(level=$level, instances=${instancesByClass.size})"
}

interface AsyncFactory<T> {
	suspend fun create(): T
}

interface InjectedHandler {
	suspend fun injectedInto(instance: Any): Unit
}

annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)

interface AsyncDependency {
	suspend fun init(): Unit
}
