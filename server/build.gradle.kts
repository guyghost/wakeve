import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    application
}

// Compile-RED proof for APNs 5.x stays independent from the unfinished 3.1 registration-store
// contracts in the canonical test source set. It is test-only and has no production runtime path.
val apnsContractTest by sourceSets.creating {
    java.srcDir("src/apnsContractTest/kotlin")
    resources.srcDir("src/apnsContractTest/resources")
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

configurations.named(apnsContractTest.implementationConfigurationName) {
    extendsFrom(configurations.testImplementation.get())
}

tasks.register<Test>("apnsContractTest") {
    description = "Runs isolated APNs 5.x compile-RED contracts without the 3.1 registration-store suite."
    group = "verification"
    testClassesDirs = apnsContractTest.output.classesDirs
    classpath = apnsContractTest.runtimeClasspath
}

// The contract test owns test doubles for internal provider tokens, just like the canonical
// Kotlin test source set. Grant only this test compilation friendship with main; it does not
// expose an API or affect the application artifact.
tasks.named<KotlinCompile>("compileApnsContractTestKotlin") {
    compilerOptions.freeCompilerArgs.add(
        "-Xfriend-paths=${layout.buildDirectory.dir("classes/kotlin/main").get().asFile.absolutePath}"
    )
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
    // Production storage fails closed without an explicit path. Tests inject an isolated
    // durable SQLite path so a newly opened store can model a worker restart.
    doFirst {
        temporaryDir.deleteRecursively()
        temporaryDir.mkdirs()
        systemProperty(
            "wakeve.notification.delivery.db.path",
            temporaryDir.resolve("notification-delivery.sqlite").absolutePath
        )
    }
}

kotlin {
    jvmToolchain(23)
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
