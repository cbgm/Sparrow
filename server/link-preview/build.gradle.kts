plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.sparrow.lint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.server.protocol)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
