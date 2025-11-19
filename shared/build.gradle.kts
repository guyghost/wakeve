plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    kotlin("plugin.serialization") version "2.2.20"
}

kotlin {
    androidTarget()
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientCio)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.iosDriver)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientCio)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jvmDriver)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientCio)
            implementation(libs.kotlinx.serialization)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.guyghost.wakeve.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

sqldelight {
    databases {
        create("WakevDb") {
            packageName.set("com.guyghost.wakeve.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight"))
        }
    }
}
