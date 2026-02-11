plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

group = "com.guyghost.wakeve"
version = "1.0.0"

application {
    mainClass.set("com.guyghost.wakeve.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    // Shared module
    implementation(projects.shared)
    
    // Ktor server
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.contentNegotiation)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverRateLimit)
    implementation(libs.ktor.serverWebsockets)
    
    // Ktor client (for server-side HTTP requests)
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    
    // SQLDelight
    implementation(libs.sqldelight.jvmDriver)
    
    // JWT
    implementation(libs.java.jwt)
    
    // Observability
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)
    
    // Logging
    implementation(libs.logback)
    
    // Testing
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
