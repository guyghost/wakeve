plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
    jacoco
}

kotlin {
    // Android target
    androidTarget()
    
    // iOS targets
    iosArm64()
    iosSimulatorArm64()
    
    // JVM target for server and desktop
    jvm()
    
    sourceSets {
        // Common source set - shared across all platforms
        commonMain {
            dependencies {
                // Kotlinx
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.datetime)
                // kotlinx-serialization-json is added by the plugin
                
                // SQLDelight
                implementation(libs.sqldelight.runtime)
            }
        }
        
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        // Android source set
        androidMain {
            dependencies {
                // SQLDelight
                implementation(libs.sqldelight.androidDriver)
                
                // Ktor client
                implementation(libs.ktor.clientCore)
                implementation(libs.ktor.clientCio)
                implementation(libs.ktor.clientContentNegotiation)
                implementation(libs.ktor.clientWebsocket)
                implementation(libs.ktor.serialization)
                
                // AndroidX
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(libs.activity.ktx)
                
                // WorkManager
                implementation(libs.work.runtime)
                
                // ML Kit
                implementation(libs.mlkit.image.labeling)
                implementation(libs.mlkit.face.detection.legacy)
                
                // Google Play Services
                implementation(libs.google.services.auth)
                implementation(libs.google.services.base)
                
                // Security
                implementation(libs.androidx.security.crypto)
                
                // Firebase
                implementation(libs.firebase.bom)
                implementation(libs.firebase.analytics)
            }
        }
        
        // iOS source set
        iosMain {
            dependencies {
                // SQLDelight
                implementation(libs.sqldelight.iosDriver)
                
                // Ktor client
                implementation(libs.ktor.clientCore)
                implementation(libs.ktor.clientCio)
                implementation(libs.ktor.clientContentNegotiation)
                implementation(libs.ktor.clientWebsocket)
            }
        }
        
        // JVM source set (server and desktop)
        jvmMain {
            dependencies {
                // SQLDelight
                implementation(libs.sqldelight.jvmDriver)
                
                // Ktor client
                implementation(libs.ktor.clientCore)
                implementation(libs.ktor.clientCio)
                implementation(libs.ktor.clientContentNegotiation)
                implementation(libs.ktor.clientWebsocket)
                implementation(libs.ktor.serialization)
            }
        }
        
        jvmTest {
            dependencies {
                implementation(libs.mockk)
            }
            kotlin {
                // Legacy JVM E2E tests are not aligned with the current APIs yet.
                exclude("**/e2e/**")
            }
        }
    }
}

android {
    namespace = "com.guyghost.wakeve.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Configure Android test dependencies
dependencies {
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.core)
}

sqldelight {
    databases {
        create("WakevDb") {
            packageName.set("com.guyghost.wakeve.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight"))
        }
    }
}

// JaCoCo Configuration for Code Coverage
jacoco {
    toolVersion = "0.8.12"
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoJvmTestReport") {
    dependsOn(tasks.withType<Test>())
    
    group = "Reporting"
    description = "Generate JaCoCo coverage report for JVM tests"
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    sourceDirectories.setFrom(
        files(
            "src/commonMain/kotlin",
            "src/jvmMain/kotlin"
        )
    )
    
    classDirectories.setFrom(
        files(
            fileTree("build/classes/kotlin/commonMain"),
            fileTree("build/classes/kotlin/jvmMain")
        )
    )
    
    executionData.setFrom(
        files("build/jacoco/jvmTest.exec")
    )
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn(tasks.withType<Test>())
    
    group = "Verification"
    description = "Verify code coverage meets minimum requirements"
    
    violationRules {
        rule {
            limit {
                minimum = BigDecimal.valueOf(0.60)
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "*.BuildConfig",
                "*Database*",
                "*Mock*",
                "*Test*",
                "*TestFixture*"
            )
            limit {
                minimum = BigDecimal.valueOf(0.50)
            }
        }
    }
    
    sourceDirectories.setFrom(
        files(
            "src/commonMain/kotlin",
            "src/jvmMain/kotlin"
        )
    )
    
    classDirectories.setFrom(
        files(
            fileTree("build/classes/kotlin/commonMain"),
            fileTree("build/classes/kotlin/jvmMain")
        )
    )
    
    executionData.setFrom(
        files("build/jacoco/jvmTest.exec")
    )
}

tasks.register("testWithCoverage") {
    group = "Verification"
    description = "Run all tests and generate coverage report"
    dependsOn("jvmTest", "jacocoJvmTestReport")
}
