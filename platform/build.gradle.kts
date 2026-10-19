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
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}