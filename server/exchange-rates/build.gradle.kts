plugins {
    kotlin("jvm")
    alias(libs.plugins.sparrow.lint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
