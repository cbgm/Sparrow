plugins {
    kotlin("jvm")
    alias(libs.plugins.sparrow.lint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.server.protocol)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}
