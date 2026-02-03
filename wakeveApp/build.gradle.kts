import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.buildconfig)
    // Google Services plugin for Firebase
    id("com.google.gms.google-services")
}

// Load local.properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
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
            // Navigation Compose
            implementation("androidx.navigation:navigation-compose:2.7.7")
            // OAuth2 dependencies
            implementation(libs.google.auth)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.workmanager)
            // Chrome Custom Tabs for Apple Sign-In web flow
            implementation("androidx.browser:browser:1.8.0")
            // Ktor client for HTTP requests
            implementation(libs.ktor.clientCore)
            implementation(libs.ktor.clientCio)
            implementation(libs.ktor.clientContentNegotiation)
            // Firebase Cloud Messaging
            implementation("com.google.firebase:firebase-messaging:24.1.0")
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
            // Koin for dependency injection (common)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            // Koin for dependency injection (Android-specific)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
            implementation("io.insert-koin:koin-androidx-compose:3.5.3")
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
            // MockK for Android instrumented tests
            implementation(libs.mockk)
            // Ktor for testing Apple Sign-In (mock engine + content negotiation + JSON serialization)
            implementation("io.ktor:ktor-client-mock:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            // Google Play Services for testing
            implementation(libs.google.auth)
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
    // Enabled for production use
    buildConfigField("Boolean", "ENABLE_OAUTH", "true")

    // Server URL for OAuth endpoints
    buildConfigField("String", "SERVER_URL", "\"http://10.0.2.2:8080\"")

    // Google OAuth Client ID - Read from local.properties
    // Get this from Google Cloud Console: https://console.cloud.google.com/
    // Add to local.properties: google.web.client.id=YOUR_ACTUAL_CLIENT_ID
    buildConfigField(
        "String",
        "GOOGLE_WEB_CLIENT_ID",
        "\"${localProperties["google.web.client.id"] as? String ?: "YOUR_GOOGLE_WEB_CLIENT_ID"}\""
    )

    // Apple OAuth Client ID - Read from local.properties
    // Add to local.properties: apple.client.id=com.yourcompany.wakeve
    buildConfigField(
        "String",
        "APPLE_CLIENT_ID",
        "\"${localProperties["apple.client.id"] as? String ?: "com.yourcompany.wakeve"}\""
    )

    // Apple OAuth Redirect URI - Read from local.properties
    // Add to local.properties: apple.redirect.uri=wakeve://apple-auth-callback
    buildConfigField(
        "String",
        "APPLE_REDIRECT_URI",
        "\"${localProperties["apple.redirect.uri"] as? String ?: "wakeve://apple-auth-callback"}\""
    )
}

// Allow local builds without Firebase config checked into the repo.
val googleServicesJson = project.file("google-services.json")
tasks.matching { it.name.matches(Regex("process\\w+GoogleServices")) }.configureEach {
    onlyIf { googleServicesJson.exists() }
}
