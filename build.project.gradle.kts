val hasAndroid: Boolean by rootProject.extra

File(projectDir, "korio/src/commonMain/kotlin/com/soywiz/korio/internal/KorioVersion.kt").apply {
	parentFile.mkdirs()
	val newText = "package com.soywiz.korio.internal\n\ninternal const val KORIO_VERSION = \"${project.property("projectVersion")}\""
	if (readText() != newText) writeText(newText)
}

val projDeps = Deps().run { mapOf(
	"korio" to listOf(kotlinxCoroutines, klock, kmem, kds)
) }

/////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////

class Deps {
	val klock = Dep("com.soywiz:klock:${project.property("klockVersion")}")
	val kmem = Dep("com.soywiz:kmem:${project.property("kmemVersion")}")
	val kds = Dep("com.soywiz:kds:${project.property("kdsVersion")}")
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

class Dep(val commonName: String? = null, val register: (DependencyHandlerScope.() -> Unit)? = null)

subprojects {
	val deps = projDeps[project.name]
	if (deps != null) {
		dependencies {
			for (dep in deps) {
				if (dep.commonName != null) {
					add("commonMainApi", dep.commonName)
					add("commonTestImplementation", dep.commonName)
				}
				dep.register?.invoke(this)
			}
		}
	}
}
