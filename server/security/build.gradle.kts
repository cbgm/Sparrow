plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.securechat.lint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.server.protocol)
    implementation(libs.bundles.serialization)
    implementation(libs.ktor.server.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.coroutines.test)
}
