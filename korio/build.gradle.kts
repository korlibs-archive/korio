//println("korio-subproject")
val hasAndroid: Boolean by rootProject.extra

File(projectDir, "src/commonMain/kotlin/com/soywiz/korio/internal/KorioVersion.kt").apply {
	parentFile.mkdirs()
	val newText = "package com.soywiz.korio.internal\n\ninternal const val KORIO_VERSION = \"${project.property("projectVersion")}\""
	if (readText() != newText) writeText(newText)
}

kotlin {
	sourceSets {
		val nonJsMain = maybeCreate("nonJsMain")

		val nativePosixAppleMain = maybeCreate("nativePosixAppleMain")
		val nativePosixNonAppleMain = maybeCreate("nativePosixNonAppleMain")
		val nativeCommonMain = maybeCreate("nativeCommonMain")
		val nativeCommonTest = maybeCreate("nativeCommonTest")
		val nativePosixMain = maybeCreate("nativePosixMain")
		val mingwX64Main = maybeCreate("mingwX64Main").apply {
			dependsOn(nativeCommonMain)
		}
		val mingwX64Test = maybeCreate("mingwX64Test").apply {
			dependsOn(nativeCommonTest)
		}

		val ios = listOf("iosX64Main", "iosArm32Main", "iosArm64Main").map { maybeCreate(it) }
		val apple = ios + listOf(maybeCreate("macosX64Main"))
		val linux = listOf(maybeCreate("linuxX64Main"))

		configure(apple) {
			dependsOn(nativePosixAppleMain)
			dependsOn(nativeCommonMain)
			dependsOn(nativePosixMain)
		}
		configure(linux) {
			dependsOn(nativePosixNonAppleMain)
			dependsOn(nativeCommonMain)
			dependsOn(nativePosixMain)
		}
		configure(apple + linux) {
			dependsOn(nativeCommonTest)
			dependsOn(nonJsMain)
		}
		configure(listOf(maybeCreate("jvmMain"))) {
			dependsOn(nonJsMain)
		}
	}
}

val coroutinesVersion: String by project
val klockVersion: String by project
val kdsVersion: String by project
val kmemVersion: String by project

dependencies {
	//jvmMainImplementation "org.java-websocket:Java-WebSocket:1.3.8"


	val coroutines = "kotlinx-coroutines-core"
	add("commonMainApi", "org.jetbrains.kotlinx:$coroutines-common:$coroutinesVersion")
	add("jvmMainApi", "org.jetbrains.kotlinx:$coroutines:$coroutinesVersion")
	add("jsMainApi", "org.jetbrains.kotlinx:$coroutines-js:$coroutinesVersion")
	if (hasAndroid) {
		//androidMainApi "org.jetbrains.kotlinx:$coroutines:$coroutinesVersion"
		add("androidMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
		add("androidTestImplementation", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
	}

	add("linuxX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_linux_x64:$coroutinesVersion")
	add("mingwX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_mingw_x64:$coroutinesVersion")
	add("macosX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_macos_x64:$coroutinesVersion")
	add("iosX64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_x64:$coroutinesVersion")
	add("iosArm32MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_arm32:$coroutinesVersion")
	add("iosArm64MainApi", "org.jetbrains.kotlinx:$coroutines-native_debug_ios_arm64:$coroutinesVersion")

	//commonMainApi "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
	add("commonMainApi", "com.soywiz:klock:$klockVersion")
	add("commonMainApi", "com.soywiz:kds:$kdsVersion")
	add("commonMainApi", "com.soywiz:kmem:$kmemVersion")

	if (hasAndroid) {
		add("commonTestImplementation", "com.soywiz:klock:$klockVersion")
		add("commonTestImplementation", "com.soywiz:kds:$kdsVersion")
		add("commonTestImplementation", "com.soywiz:kmem:$kmemVersion")
	}
}
