plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

// Use environment variables for configuration (set via env or test config)
val jwtSecret = System.getenv("JWT_SECRET") ?: "dev-secret-change-in-production"
val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"

// Test configuration
tasks.withType<Test> {
    environment = mapOf(
        "JWT_SECRET" to jwtSecret,
        "JWT_ISSUER" to jwtIssuer,
        "JWT_AUDIENCE" to jwtAudience
    )
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
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
    
    // kotlinx-datetime
    implementation(libs.kotlinx.datetime)

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
