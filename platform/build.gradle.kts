plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationJson)
        }
        jvmMain.dependencies {
            implementation(libs.jna)
            implementation(libs.dbus.java.core)
            implementation(libs.dbus.java.native)
            implementation(libs.slf4j.nop)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}