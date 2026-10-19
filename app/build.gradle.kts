import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":platform"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
}

compose.desktop {
    application {
        mainClass = "id.andreasmlbngaol.xtray.app.MainKt"

        buildTypes.release {
            proguard {
                isEnabled = true
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            // dbus-java authenticates with com.sun.security.auth.module.UnixSystem,
            // which lives in jdk.security.auth and is not part of the default image.
            modules("jdk.security.auth", "java.security.sasl")
            packageName = "x-tray"
            packageVersion = providers.gradleProperty("appVersion").getOrElse("1.0.0")
            description = "Dynamic Island untuk desktop Linux"
            vendor = "X-Tray"
            copyright = "(c) 2026 X-Tray"

            linux {
                iconFile.set(project.file("icons/x-tray.png"))
                debMaintainer = "x-tray@localhost"
                appCategory = "Utility"
                menuGroup = "Utility"
                shortcut = true
            }
        }
    }
}
