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
            // jpackage can only build for the host OS: Linux produces the .deb,
            // Windows produces the .msi. CI runs one job per platform.
            targetFormats(TargetFormat.Deb, TargetFormat.Msi)
            // dbus-java authenticates with com.sun.security.auth.module.UnixSystem,
            // which lives in jdk.security.auth and is not part of the default image.
            modules("jdk.security.auth", "java.security.sasl")
            packageName = "x-tray"
            packageVersion = providers.gradleProperty("appVersion").getOrElse("1.0.0")
            description = "A Dynamic Island for your desktop"
            vendor = "X-Tray"
            copyright = "(c) 2026 X-Tray"

            linux {
                iconFile.set(project.file("icons/x-tray.png"))
                debMaintainer = "x-tray@localhost"
                appCategory = "Utility"
                menuGroup = "Utility"
                shortcut = true
            }

            windows {
                iconFile.set(project.file("icons/x-tray.ico"))
                menuGroup = "X-Tray"
                shortcut = true
                perUserInstall = true
                dirChooser = true
                // Stable across releases so an upgrade replaces the old install.
                upgradeUuid = "5f1a7f2c-3d0e-4f0a-9b7e-6c1d2a8e4b51"
            }
        }
    }
}
