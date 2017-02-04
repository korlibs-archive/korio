package com.soywiz.korio.util

import java.lang.reflect.Field

val Class<*>.allDeclaredFields: List<Field> get() = this.declaredFields.toList() + (this.superclass?.allDeclaredFields?.toList() ?: listOf<Field>())