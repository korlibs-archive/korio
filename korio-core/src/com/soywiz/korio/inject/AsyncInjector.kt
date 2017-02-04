package com.soywiz.korio.inject

import com.jtransc.annotation.JTranscKeep
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.allDeclaredFields
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

class AsyncInjector(val parent: AsyncInjector? = null, val level: Int = 0) {
	private val instances = hashMapOf<Class<*>, Any?>()

	fun child() = AsyncInjector(this, level + 1)

	suspend inline fun <reified T : Any> get() = get(T::class.java)

	inline fun <reified T : Any> map(instance: T): AsyncInjector = map(T::class.java, instance)
	fun <T : Any?> map(clazz: Class<T>, instance: T): AsyncInjector = this.apply { instances[clazz] = instance as Any }

	init {
		map<AsyncInjector>(this)
	}

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any?> get(clazz: Class<T>): T = getOrNull(clazz) ?: create(clazz)

	@Suppress("UNCHECKED_CAST")
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
			// @TODO: Performance: Cache all this!
			// Use: ClassFactory and stuff

			val loaderClass = clazz.getAnnotation(AsyncFactoryClass::class.java)
			val constructor = if (loaderClass != null) {
				loaderClass.clazz.java.declaredConstructors.firstOrNull() ?: invalidOp("Class '$clazz' doesn't have constructors")
			} else {
				clazz.declaredConstructors.firstOrNull() ?: invalidOp("Class '$clazz' doesn't have constructors")
			}
			val out = arrayListOf<Any?>()

			for ((paramType, annotations) in constructor.parameterTypes.zip(constructor.parameterAnnotations)) {
				val i = if (annotations.isNotEmpty()) {
					val i = this.child()
					for (annotation in annotations) {
						i.map(annotation.annotationClass.java as Class<Any>, annotation as Any)
					}
					i
				} else {
					this
				}
				out += i.get(paramType)
			}
			val instance = constructor.newInstance(*out.toTypedArray())

			//println("Inject.create(${clazz.name}):")
			//for (field in clazz.fields) {
			//	println("Inject.create(${clazz.name}): ${field.name} : ${field.type}")
			//}
//
			//	// Injections based on @Inject to fields
			//for (field in clazz.fields.filter { it.getAnnotation(Inject::class.java) != null }) {
			//	println("Inject.create(YES)(${clazz.name}): ${field.name} : ${field.type}")
			//	field.isAccessible = true
			//	field.set(instance, this.get(field.type))
			//}

			val allDeclaredFields = clazz.allDeclaredFields

			//for (field in allDeclaredFields) {
			//	val annotation = field.getAnnotation(Inject::class.java)
			//	val annotationCount = field.declaredAnnotations.size
			//	println("Inject.create(${clazz.name}): ${field.name} : ${field.type} : ${annotation != null} : $annotationCount")
			//}

			// @TODO: Cache this!
			for (field in allDeclaredFields.filter { it.getAnnotation(Inject::class.java) != null }) {
				field.isAccessible = true
				field.set(instance, this.get(field.type))
			}

			//for (method in clazz.methods) {
			//	println(method.name + " : " + method.annotations.toList().count())
			//}

			if (instance is AsyncDependency) {
				instance.init()
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

	override fun toString(): String = "AsyncInjector(level=$level, instances=${instances.size})"
}

interface AsyncFactory<T> {
	suspend fun create(): T
}

annotation class AsyncFactoryClass(val clazz: KClass<out AsyncFactory<*>>)

interface AsyncDependency {
	suspend fun init(): Unit
}
