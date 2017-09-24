package com.soywiz.korio.inject

import com.soywiz.korio.lang.KClass

fun AsyncInjector.jvmAutomapping(): AsyncInjector = this.apply {
	this.fallbackProvider = object : AsyncObjectProvider() {
		suspend override fun getType(clazz: KClass<*>): Type {
			TODO("not implemented")
			return Type.None
		}

		suspend override fun <T : Any> createInstance(injector: AsyncInjector, clazz: KClass<T>): T {
			TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
			/*
				val isPrototype = clazz.java.getAnnotation(Prototype::class.java) != null
				val isSingleton = clazz.java.getAnnotation(Singleton::class.java) != null
				val isAsyncFactoryClass = clazz.java.getAnnotation(AsyncFactoryClass::class.java) != null

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
						out += i.getOrNull(paramType, ctx) ?: throw AsyncInjector.NotMappedException(paramType, actualClass, ctx)
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
			*/
		}
	}
}