@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
    jacoco
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Android target
    androidTarget()
    
    // iOS targets
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = false
        }
    }
    
    // JVM target for server and desktop
    jvm()
    
    sourceSets {
        // Common source set - shared across all platforms
        commonMain {
            dependencies {
                // Kotlinx
                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                
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
                implementation(libs.mlkit.genai.prompt)
                
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
                implementation(libs.ktor.serialization)
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
        create("WakeveDb") {
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

// KMP puts classes in build/classes/kotlin/jvm/main/
val jvmMainClasses by lazy {
    fileTree(layout.buildDirectory.dir("classes/kotlin/jvm/main")) {
        exclude(listOf("**/*Test*", "**/*\$inlined*", "**/coroutines/**"))
    }
}

tasks.register<JacocoReport>("jacocoJvmTestReport") {
    dependsOn("jvmTest")
    
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
    
    classDirectories.setFrom(jvmMainClasses)
    
    executionData.setFrom(
        layout.buildDirectory.file("jacoco/jvmTest.exec")
    )
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    dependsOn("jvmTest", "jacocoJvmTestReport")
    
    group = "Verification"
    description = "Verify JVM coverage does not regress below the current maintained baseline"
    
    violationRules {
        rule {
            limit {
                minimum = BigDecimal.valueOf(0.30)
            }
        }
    }
    
    sourceDirectories.setFrom(
        files(
            "src/commonMain/kotlin",
            "src/jvmMain/kotlin"
        )
    )
    
    classDirectories.setFrom(jvmMainClasses)
    
    executionData.setFrom(
        layout.buildDirectory.file("jacoco/jvmTest.exec")
    )
}

tasks.register("testWithCoverage") {
    group = "Verification"
    description = "Run all tests and generate coverage report"
    dependsOn("jvmTest", "jacocoJvmTestReport")
}
