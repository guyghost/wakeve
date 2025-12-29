import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.buildconfig)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm()
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.activity.compose)
            // OAuth2 dependencies
            implementation(libs.google.auth)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.workmanager)
            // Ktor client for HTTP requests
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientCio)
            implementation(libs.ktor.clientContentNegotiation)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // Koin for dependency injection (Android-specific)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidInstrumentedTest.dependencies {
            // Android instrumented test dependencies
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.espresso.core)
            implementation(libs.kotlin.test)
            // Add androidx.test:core directly for ApplicationProvider
            implementation("androidx.test:core:1.5.0")
            // Compose UI testing
            implementation("androidx.compose.ui:ui-test-junit4:1.6.0")
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // Ktor client for HTTP requests
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientCio)
            implementation(libs.ktor.clientContentNegotiation)
            // Ktor server for OAuth callback listener
            implementation(libs.ktor.serverCore)
            implementation(libs.ktor.serverNetty)
        }
    }
}

android {
    namespace = "com.guyghost.wakeve"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.guyghost.wakeve"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.guyghost.wakeve.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.guyghost.wakeve"
            packageVersion = "1.0.0"
        }
    }
}

buildConfig {
    packageName("com.guyghost.wakeve")

    // Feature flag for progressive OAuth rollout
    // Set to false initially for safe migration
    buildConfigField("Boolean", "ENABLE_OAUTH", "false")

    // Server URL for OAuth endpoints
    buildConfigField("String", "SERVER_URL", "\"http://10.0.2.2:8080\"")
}
