plugins {
    kotlin("jvm")
    alias(libs.plugins.securechat.lint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
