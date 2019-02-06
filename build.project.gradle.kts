val hasAndroid: Boolean by rootProject.extra

val pname = "korio"

File(projectDir, "$pname/src/commonMain/kotlin/com/soywiz/$pname/internal/${pname.capitalize()}Version.kt").apply {
	parentFile.mkdirs()
	val newText = "package com.soywiz.$pname.internal\n\ninternal const val ${pname.toUpperCase()}_VERSION = \"${project.version}\""
	if (!exists() || (readText() != newText)) writeText(newText)
}

val projDeps = Deps().run { LinkedHashMap<String, List<Dep>>().apply {
	this["korio"] = listOf(kotlinxCoroutines, klock, kmem, kds)
} }

/////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////

class Deps {
	val klock = DepKorlib("klock")
	val kmem = DepKorlib("kmem")
	val kds = DepKorlib("kds")
	val kotlinxCoroutines = DepKorlib("kotlinx-coroutines")
	//val kotlinxCoroutines = Dep {
	//	val coroutinesVersion: String by project
	//	val coroutines = "kotlinx-coroutines-core"
	//	add("commonMainApi", "org.jetbrains.kotlinx:$coroutines-common:$coroutinesVersion")
	//	add("jvmMainApi", "org.jetbrains.kotlinx:$coroutines:$coroutinesVersion")
	//	add("jsMainApi", "org.jetbrains.kotlinx:$coroutines-js:$coroutinesVersion")
	//	if (hasAndroid) {
	//		add("androidMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
	//		add("androidTestImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
	//	}
	//	add("linuxX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_linux_x64:$coroutinesVersion")
	//	add("mingwX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_mingw_x64:$coroutinesVersion")
	//	add("macosX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_macos_x64:$coroutinesVersion")
	//	add("iosX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_x64:$coroutinesVersion")
	//	add("iosArm32MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_arm32:$coroutinesVersion")
	//	add("iosArm64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_arm64:$coroutinesVersion")
	//	addCommon("org.jetbrains", "kotlinx:atomicfu", "0.12.1", commonRename = true, androidIsJvm = true)
	//}
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////

fun DepKorlib(name: String) = Dep("com.soywiz:$name:${project.property("${name}Version")}")
class Dep(val commonName: String? = null, val project: String ? = null, val register: (DependencyHandlerScope.() -> Unit)? = null)

val ALL_TARGETS = listOf("android", "iosArm64", "iosArm32", "iosX64", "js", "jvm", "linuxX64", "macosX64", "mingwX64", "metadata")

fun DependencyHandler.addCommon(group: String, name: String, version: String, targets: List<String> = ALL_TARGETS, suffixCommonRename: Boolean = false, androidIsJvm: Boolean = true) {
	for (target in targets) {
		val base = when (target) {
			"metadata" -> "common"
			else -> target
		}
		val suffix = when {
			target == "android" && androidIsJvm -> "-jvm"
			target == "metadata" && suffixCommonRename -> "-common"
			else -> "-${target.toLowerCase()}"
		}

		val packed = "$group:$name$suffix:$version"
		add("${base}MainApi", packed)
		add("${base}TestImplementation", packed)
	}
}

fun DependencyHandler.addCommon(dependency: String, targets: List<String> = ALL_TARGETS) {
	val (group, name, version) = dependency.split(":", limit = 3)
	return addCommon(group, name, version, targets)
}

subprojects {
	val deps = projDeps[project.name]
	if (deps != null) {
		dependencies {
			for (dep in deps) {
				if (dep.commonName != null) {
					addCommon(dep.commonName)
				}
				if (dep.project != null) {
					add("commonMainApi", rootProject.project(dep.project))
					add("commonTestImplementation", rootProject.project(dep.project))
				}
				dep.register?.invoke(this)
			}
		}
	}
}
