plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val appVersion = providers.gradleProperty("appVersion").getOrElse("1.0.0")

/**
 * Single source of truth for the version: the tag drives it in CI, local builds
 * fall back to gradle.properties.
 */
val appInfoDir = layout.buildDirectory.dir("generated/appInfo/kotlin")

val generateAppInfo by tasks.registering {
    val outputDir = appInfoDir
    val version = appVersion
    inputs.property("appVersion", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("id/andreasmlbngaol/xtray/core/AppInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package id.andreasmlbngaol.xtray.core

            object AppInfo {
                const val NAME = "X-Tray"
                const val VERSION = "$version"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            kotlin.srcDir(generateAppInfo)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.kotlinx.serializationJson)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}