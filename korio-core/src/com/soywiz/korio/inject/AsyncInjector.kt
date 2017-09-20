package com.soywiz.korio.inject

import com.jtransc.annotation.JTranscKeep
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.allDeclaredFields
import java.lang.reflect.Modifier
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

class AsyncInjector(val parent: AsyncInjector? = null, val level: Int = 0) : Extra by Extra.Mixin() {
	private val instancesByClass = hashMapOf<KClass<*>, Any?>()
	var defaultProvider: (suspend (clazz: KClass<*>) -> Any)? = null

	fun child() = AsyncInjector(this, level + 1)

	val rootForSingleton: AsyncInjector = parent?.rootForSingleton ?: this

	suspend inline fun <reified T : Any> get() = get(T::class)

	inline fun <reified T : Any> mapTyped(instance: T): AsyncInjector = map(instance, T::class)
	fun <T : Any> map(instance: T, clazz: KClass<T> = instance.javaClass): AsyncInjector = this.apply { instancesByClass[clazz] = instance as Any }

	init {
		mapTyped<AsyncInjector>(this)
	}

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any> get(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T = getOrNull(clazz, ctx) ?: create(clazz, ctx) ?: invalidOp("Class '$clazz' doesn't have constructors $ctx")

	data class RequestContext(val initialClazz: KClass<*>)

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any?> getOrNull(clazz: KClass<T>, ctx: RequestContext = RequestContext(clazz)): T? {
		return if (clazz.getAnnotation(Prototype::class) != null) {
			val instance = create(clazz, ctx)
			instancesByClass[clazz] = instance
			instance
		} else if (clazz.getAnnotation(Singleton::class) != null) {
			val root = rootForSingleton
			//val root = this
			if (!has(clazz)) {
				val instance = create(clazz, ctx) ?: return null
				root.instancesByClass[clazz] = instance
				instance
			} else {
				(instancesByClass[clazz] ?: parent?.getOrNull(clazz, ctx)) as T?
			}
		} else if (clazz.getAnnotation(AsyncFactoryClass::class) != null) {
			create(clazz, ctx)
		} else {
			(instancesByClass[clazz] ?: parent?.getOrNull(clazz, ctx)) as T?
		}
	}

	fun has(clazz: Class<*>): Boolean = instancesByClass.containsKey(clazz) || parent?.has(clazz) ?: false

	@Suppress("UNCHECKED_CAST")
	@JvmOverloads
	suspend fun <T : Any?> create(clazz: Class<T>, ctx: RequestContext = RequestContext(clazz)): T? {
		try {
			// @TODO: Performance: Cache all this!
			// Use: ClassFactory and stuff

			val loaderClass = clazz.getAnnotation(AsyncFactoryClass::class.java)
			val actualClass = loaderClass?.clazz?.java ?: clazz
			if (actualClass.isInterface || Modifier.isAbstract(actualClass.modifiers)) invalidOp("Can't instantiate abstract or interface: $actualClass in $ctx")
			val constructor = actualClass.declaredConstructors.firstOrNull() ?: return null
			val out = arrayListOf<Any?>()
			val allInstances = arrayListOf<Any?>()

			for ((paramType, annotations) in constructor.parameterTypes.zip(constructor.parameterAnnotations)) {
				var isOptional = false

				val i = if (annotations.isNotEmpty()) {
					val i = this.child()
					for (annotation in annotations) {
						when (annotation) {
							is Optional -> isOptional = true
							else -> i.map(annotation as Any, annotation.annotationClass.java as Class<Any>)
						}
					}
					i
				} else {
					this
				}
				if (isOptional) {
					out += if (i.has(paramType)) i.getOrNull(paramType, ctx) else null
				} else {
					out += i.getOrNull(paramType, ctx) ?: throw NotMappedException(paramType, actualClass, ctx)
				}
			}
			allInstances.addAll(out)
			constructor.isAccessible = true
			val instance = constructor.newInstance(*out.toTypedArray())

			val allDeclaredFields = clazz.allDeclaredFields

			// @TODO: Cache this!
			for (field in allDeclaredFields.filter { it.getAnnotation(Inject::class.java) != null }) {
				if (Modifier.isStatic(field.modifiers)) continue
				var isOptional = false
				val i = if (field.annotations.isNotEmpty()) {
					val i = this.child()
					for (annotation in field.annotations) {
						when (annotation) {
							is Optional -> isOptional = true
							else -> i.map(annotation as Any, annotation.annotationClass.java as Class<Any>)
						}
					}
					i
				} else {
					this
				}
				field.isAccessible = true
				val res = if (isOptional) {
					if (i.has(field.type)) i.get(field.type, ctx) else null
				} else {
					i.get(field.type, ctx)
				}
				allInstances += res
				field.set(instance, res)
			}

			if (instance is AsyncDependency) instance.init()

			for (createdInstance in allInstances) {
				if (createdInstance is InjectedHandler) {
					createdInstance.injectedInto(instance)
				}
			}

			if (loaderClass != null) {
				return (instance as AsyncFactory<T>).create()
			} else {
				return instance as T
			}
		} catch (e: Throwable) {
			println("$this error while creating '$clazz': (${e.message}):")
			e.printStackTrace()
			throw e
		}
	}

	class NotMappedException(val clazz: KClass<*>, val requestedByClass: KClass<*>, val ctx: RequestContext) : RuntimeException("Not mapped ${clazz.name} requested by ${requestedByClass.name} in $ctx")

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
