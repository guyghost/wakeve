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
    alias(libs.plugins.kotlinSerialization)
    id("com.google.gms.google-services")
}

// Load local.properties file
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val enableDesktopTarget = providers.gradleProperty("enableDesktopTarget")
    .orNull
    ?.toBooleanStrictOrNull() == true

val serverUrl = (localProperties["server.url"] as? String)
    ?: providers.gradleProperty("wakeve.serverUrl").orNull
    ?: "https://api.wakeve.app"
val escapedServerUrl = serverUrl.replace("\\", "\\\\").replace("\"", "\\\"")

// Release signing: set in local.properties (gitignored) or CI environment variables.
// RELEASE_STORE_FILE=path/to/upload-keystore.jks
// RELEASE_STORE_PASSWORD=...
// RELEASE_KEY_ALIAS=...
// RELEASE_KEY_PASSWORD=...
fun readReleaseSigningProperty(name: String): String? =
    (localProperties[name] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

val releaseStoreFile = readReleaseSigningProperty("RELEASE_STORE_FILE")
val releaseStorePassword = readReleaseSigningProperty("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = readReleaseSigningProperty("RELEASE_KEY_ALIAS")
val releaseKeyPassword = readReleaseSigningProperty("RELEASE_KEY_PASSWORD")
val hasReleaseSigningConfig = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

kotlin {
    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    // Desktop JVM target (optional)
    if (enableDesktopTarget) {
        jvm()
    }
    
    // Apply the default hierarchy template
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    applyDefaultHierarchyTemplate()
    
    sourceSets {
        // Common source set - shared across all platforms
        commonMain {
            dependencies {
                // Shared module
                implementation(projects.shared)
                
                // Compose
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                
                // Lifecycle
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                
                // Kotlinx
                implementation(libs.kotlinx.datetime)
                // kotlinx-serialization is already included via shared module
                
                // Koin DI
                implementation(libs.koin.core)
            }
        }
        
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
            kotlin {
                // Temporarily exclude Android-only Compose test from common unit-test compilation
                exclude("**/ui/components/AIBadgeTest.kt")
            }
        }
        
        // Android source set
        androidMain {
            dependencies {
                // Compose Android
                implementation(compose.preview)
                implementation(compose.materialIconsExtended)
                implementation(libs.androidx.activity.compose)
                
                // Navigation
                implementation(libs.navigation.compose)
                
                // OAuth & Auth
                implementation(libs.google.auth)
                implementation(libs.androidx.credentials)
                
                // Security & WorkManager
                implementation(libs.androidx.security.crypto)
                implementation(libs.androidx.workmanager)
                
                // Browser for OAuth flows
                implementation(libs.browser)
                
                // Ktor client
                implementation(libs.ktor.clientCore)
                implementation(libs.ktor.clientCio)
                implementation(libs.ktor.clientContentNegotiation)
                implementation(libs.ktor.serialization)

                // Firebase
                implementation(libs.firebase.messaging)
                
                // Image loading
                implementation(libs.coil.compose)
                implementation(libs.coil.svg)
                implementation(libs.zxing.core)
                
                // Kotlinx Serialization
                implementation(libs.kotlinx.serialization.json)
                
                // Koin Android
                implementation(libs.koin.core)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
            }
        }
        
        androidUnitTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
            kotlin {
                // Temporarily exclude JVM/Mockito-heavy test until migrated to KMP-friendly stack
                exclude("**/viewmodel/SmartAlbumsViewModelTest.kt")
            }
        }
        
        androidInstrumentedTest {
            dependencies {
                implementation(libs.androidx.testExt.junit)
                implementation(libs.androidx.espresso.core)
                implementation(libs.kotlin.test)
                implementation(libs.androidx.test.core)
                implementation(libs.compose.ui.test)
                implementation(libs.mockk)
                
                // Ktor for testing
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.clientContentNegotiation)
                implementation(libs.ktor.serialization)
                
                // Google Play Services
                implementation(libs.google.auth)
            }
        }
        
        // Desktop JVM source set (optional)
        if (enableDesktopTarget) {
            jvmMain {
                dependencies {
                    implementation(compose.desktop.currentOs)
                    implementation(libs.kotlinx.coroutinesSwing)
                    
                    // Ktor client
                    implementation(libs.ktor.clientCore)
                    implementation(libs.ktor.clientCio)
                    implementation(libs.ktor.clientContentNegotiation)
                    
                    // Ktor server for OAuth callback
                    implementation(libs.ktor.serverCore)
                    implementation(libs.ktor.serverNetty)
                }
            }
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
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
    
    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    lint {
        baseline = file("lint-baseline.xml")
        checkReleaseBuilds = false
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Desktop configuration
if (enableDesktopTarget) {
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
}

// BuildConfig configuration
buildConfig {
    packageName("com.guyghost.wakeve")
    
    // Feature flag for progressive OAuth rollout
    buildConfigField("Boolean", "ENABLE_OAUTH", "true")
    
    // Server URL for OAuth, invitation, and notification endpoints.
    buildConfigField("String", "SERVER_URL", "\"$escapedServerUrl\"")

    // App metadata surfaced in settings/about UI.
    buildConfigField("String", "VERSION_NAME", "\"${android.defaultConfig.versionName}\"")
    buildConfigField("Int", "VERSION_CODE", "${android.defaultConfig.versionCode}")
    
    // Google OAuth Client ID
    buildConfigField(
        "String",
        "GOOGLE_WEB_CLIENT_ID",
        "\"${localProperties["google.web.client.id"] as? String ?: "YOUR_GOOGLE_WEB_CLIENT_ID"}\""
    )
    
    // Apple OAuth Client ID
    buildConfigField(
        "String",
        "APPLE_CLIENT_ID",
        "\"${localProperties["apple.client.id"] as? String ?: "com.yourcompany.wakeve"}\""
    )
    
    // Apple OAuth Redirect URI
    buildConfigField(
        "String",
        "APPLE_REDIRECT_URI",
        "\"${localProperties["apple.redirect.uri"] as? String ?: "wakeve://apple-auth-callback"}\""
    )

    // Google Maps Platform Weather API key (optional Android weather provider).
    buildConfigField(
        "String",
        "GOOGLE_MAPS_API_KEY",
        "\"${localProperties["google.maps.api.key"] as? String ?: ""}\""
    )
}

// Allow local builds without Firebase config checked into the repo
val googleServicesJson = project.file("google-services.json")
tasks.matching { it.name.matches(Regex("process\\w+GoogleServices")) }.configureEach {
    onlyIf { googleServicesJson.exists() }
}
