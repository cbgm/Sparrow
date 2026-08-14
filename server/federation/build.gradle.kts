plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.securechat.lint)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.cbgm.securechat.server.federation.ApplicationKt")
}

dependencies {
    implementation(projects.server.protocol)
    implementation(projects.server.security)
    implementation(projects.server.persistence)
    implementation(projects.server.observability)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.logback.classic)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
}
