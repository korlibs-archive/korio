package com.soywiz.korio.inject

import com.soywiz.korio.error.invalidOp
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Prototype

@Target(AnnotationTarget.CLASS)
annotation class Singleton

class AsyncInjector(val parent: AsyncInjector? = null, val level: Int = 0) {
	private val instances = hashMapOf<Class<*>, Any?>()

	fun child() = AsyncInjector(this, level + 1)

	suspend inline fun <reified T : Any> get() = get(T::class.java)

	inline fun <reified T : Any> map(instance: T): AsyncInjector = map(T::class.java, instance)

	init {
		map<AsyncInjector>(this)
	}

	fun <T : Any?> map(clazz: Class<T>, instance: T): AsyncInjector {
		instances[clazz] = instance as Any
		return this
	}

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any?> get(clazz: Class<T>): T {
		return getOrNull(clazz) ?: create(clazz)
	}

	suspend fun <T : Any?> getOrNull(clazz: Class<T>): T? {
		return if (instances.containsKey(clazz) || clazz.getAnnotation(Singleton::class.java) != null) {
			if (!instances.containsKey(clazz)) {
				val instance = create(clazz)
				instances[clazz] = instance
			}
			instances[clazz]!! as T
		} else {
			parent?.getOrNull(clazz)
		}
	}

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any?> create(clazz: Class<T>): T {
		try {
			val loaderClass = clazz.getAnnotation(LoaderClass::class.java)
			val constructor = if (loaderClass != null) {
				loaderClass.clazz.java.declaredConstructors.firstOrNull() ?: invalidOp("Class '$clazz' doesn't have constructors")
			} else {
				clazz.declaredConstructors.firstOrNull() ?: invalidOp("Class '$clazz' doesn't have constructors")
			}
			val out = arrayListOf<Any>()

			for ((paramType, annotations) in constructor.parameterTypes.zip(constructor.parameterAnnotations)) {
				if (annotations.isNotEmpty()) {
					val i = this.child()
					for (annotation in annotations) {
						i.map(annotation.annotationClass.java as Class<Any>, annotation as Any)
					}
					out += i.get(paramType)
				} else {
					out += get(paramType)
				}
			}
			val instance = constructor.newInstance(*out.toTypedArray())
			if (instance is AsyncDependency) {
				instance.init()
			}
			if (loaderClass != null) {
				return (instance as Loader<T>).load()
			} else {
				return instance as T
			}
		} catch (e: Throwable) {
			println("$this error while creating '$clazz': (${e.message}):")
			e.printStackTrace()
			throw e
		}
	}

	override fun toString(): String = "AsyncInjector(level=$level, instances=${instances.size})"
}

interface Loader<T> {
	suspend fun load(): T
}

annotation class LoaderClass(val clazz: KClass<out Loader<*>>)

interface AsyncDependency {
	suspend fun init(): Unit
}
