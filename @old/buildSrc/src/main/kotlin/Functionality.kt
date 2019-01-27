import java.io.*
import org.gradle.api.*

val Project.hasAndroid get() =
	(System.getProperty("sdk.dir") != null) || (System.getenv("ANDROID_HOME") != null) || (File(rootDir, "local.properties").exists())