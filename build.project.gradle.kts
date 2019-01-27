val hasAndroid: Boolean by rootProject.extra

val pname = "korio"

File(projectDir, "$pname/src/commonMain/kotlin/com/soywiz/$pname/internal/${pname.capitalize()}Version.kt").apply {
	parentFile.mkdirs()
	val newText = "package com.soywiz.$pname.internal\n\ninternal const val ${pname.toUpperCase()}_VERSION = \"${project.property("projectVersion")}\""
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
	val kotlinxCoroutines = Dep {
		val coroutinesVersion: String by project
		val coroutines = "kotlinx-coroutines-core"
		add("commonMainApi", "org.jetbrains.kotlinx:$coroutines-common:$coroutinesVersion")
		add("jvmMainApi", "org.jetbrains.kotlinx:$coroutines:$coroutinesVersion")
		add("jsMainApi", "org.jetbrains.kotlinx:$coroutines-js:$coroutinesVersion")
		if (hasAndroid) {
			add("androidMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
			add("androidTestImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
		}
		add("linuxX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_linux_x64:$coroutinesVersion")
		add("mingwX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_mingw_x64:$coroutinesVersion")
		add("macosX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_macos_x64:$coroutinesVersion")
		add("iosX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_x64:$coroutinesVersion")
		add("iosArm32MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_arm32:$coroutinesVersion")
		add("iosArm64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_arm64:$coroutinesVersion")
	}
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////

fun DepKorlib(name: String) = Dep("com.soywiz:$name:${project.property("${name}Version")}")
class Dep(val commonName: String? = null, val project: String ? = null, val register: (DependencyHandlerScope.() -> Unit)? = null)

subprojects {
	val deps = projDeps[project.name]
	if (deps != null) {
		dependencies {
			for (dep in deps) {
				if (dep.commonName != null) {
					add("commonMainApi", dep.commonName)
					add("commonTestImplementation", dep.commonName)
				}
				if (dep.project != null) {
					add("commonMainApi", project(dep.project))
					add("commonTestImplementation", project(dep.project))
				}
				dep.register?.invoke(this)
			}
		}
	}
}
