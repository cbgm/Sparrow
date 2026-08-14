plugins {
    kotlin("jvm")
    alias(libs.plugins.sparrow.lint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.server.protocol)
    testImplementation(kotlin("test"))
}
