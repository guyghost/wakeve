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
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientCio)
            implementation(libs.ktor.clientWebsocket)
            implementation(libs.androidx.core.ktx)
            // Activity Result API for image picker
            implementation("androidx.activity:activity-ktx:1.9.3")
            implementation("androidx.appcompat:appcompat:1.7.0")
            // ML Kit Vision for on-device photo recognition
            implementation("com.google.android.gms:play-services-mlkit-image-labeling:16.0.8")
            implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
            // Android Security - EncryptedSharedPreferences
            implementation("androidx.security:security-crypto:1.1.0-alpha06")
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.iosDriver)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientCio)
            implementation(libs.ktor.clientWebsocket)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jvmDriver)
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientContentNegotiation)
            implementation(libs.ktor.clientCio)
            implementation(libs.ktor.clientWebsocket)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockk)
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
