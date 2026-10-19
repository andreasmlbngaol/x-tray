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
        mainClass = "id.andreasmlbngaol.islanddesk.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "islanddesk"
            packageVersion = "0.1.0"
        }
    }
}