plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "com.guyghost.wakeve"
version = "1.0.0"
application {
    mainClass.set("com.guyghost.wakeve.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.contentNegotiation)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    implementation(libs.sqldelight.jvmDriver)
    implementation(libs.java.jwt)
    implementation(libs.ktor.serverRateLimit)
    implementation(libs.ktor.serverWebsockets)
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}