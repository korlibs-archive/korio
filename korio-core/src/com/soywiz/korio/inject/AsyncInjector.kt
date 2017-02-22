package com.soywiz.korio.inject

import com.jtransc.annotation.JTranscKeep
import com.soywiz.korio.error.invalidOp
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
@JTranscKeep
annotation class Inject

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class Optional

class AsyncInjector(val parent: AsyncInjector? = null, val level: Int = 0) {
	private val instancesByClass = hashMapOf<Class<*>, Any?>()

	fun child() = AsyncInjector(this, level + 1)

	suspend inline fun <reified T : Any> get() = get(T::class.java)

	inline fun <reified T : Any> map(instance: T): AsyncInjector = map(T::class.java, instance)
	fun <T : Any?> map(clazz: Class<T>, instance: T): AsyncInjector = this.apply { instancesByClass[clazz] = instance as Any }

	init {
		map<AsyncInjector>(this)
	}

	@Suppress("UNCHECKED_CAST")
	@JvmOverloads
	suspend fun <T : Any?> get(clazz: Class<T>, ctx: RequestContext = RequestContext(clazz)): T = getOrNull(clazz, ctx) ?: create(clazz, ctx) ?: invalidOp("Class '$clazz' doesn't have constructors $ctx")

	data class RequestContext(val initialClazz: Class<*>)

	@Suppress("UNCHECKED_CAST")
	@JvmOverloads
	suspend fun <T : Any?> getOrNull(clazz: Class<T>, ctx: RequestContext = RequestContext(clazz)): T? {
		return if (clazz.getAnnotation(Prototype::class.java) != null) {
			val instance = create(clazz, ctx)
			instancesByClass[clazz] = instance
			instance
		} else if (clazz.getAnnotation(Singleton::class.java) != null) {
			if (!has(clazz)) {
				val instance = create(clazz, ctx) ?: return null
				instancesByClass[clazz] = instance
			}
			(instancesByClass[clazz] ?: parent?.getOrNull(clazz, ctx)) as T?
		} else {
			(instancesByClass[clazz] ?: parent?.getOrNull(clazz, ctx)) as T?
		}
	}

	suspend fun has(clazz: Class<*>): Boolean = instancesByClass.containsKey(clazz) || parent?.has(clazz) ?: false

	@Suppress("UNCHECKED_CAST")
	@JvmOverloads
	suspend fun <T : Any?> create(clazz: Class<T>, ctx: RequestContext = RequestContext(clazz)): T? {
		try {
			// @TODO: Performance: Cache all this!
			// Use: ClassFactory and stuff

			val loaderClass = clazz.getAnnotation(AsyncFactoryClass::class.java)
			val constructor = if (loaderClass != null) {
				loaderClass.clazz.java.declaredConstructors.firstOrNull() ?: return null
			} else {
				clazz.declaredConstructors.firstOrNull() ?: return null
			}
			val out = arrayListOf<Any?>()

			for ((paramType, annotations) in constructor.parameterTypes.zip(constructor.parameterAnnotations)) {
				var isOptional = false

				val i = if (annotations.isNotEmpty()) {
					val i = this.child()
					for (annotation in annotations) {
						when (annotation) {
							is Optional -> isOptional = true
							else -> i.map(annotation.annotationClass.java as Class<Any>, annotation as Any)
						}
					}
					i
				} else {
					this
				}
				if (isOptional) {
					out += if (i.has(paramType)) i.get(paramType, ctx) else null
				} else {
					out += i.get(paramType, ctx)
				}
			}
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
							else -> i.map(annotation.annotationClass.java as Class<Any>, annotation as Any)
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
				field.set(instance, res)
			}

			if (instance is AsyncDependency) instance.init()

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

	override fun toString(): String = "AsyncInjector(level=$level, instances=${instancesByClass.size})"
}

interface AsyncFactory<T> {
	suspend fun create(): T
}

annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)

interface AsyncDependency {
	suspend fun init(): Unit
}
