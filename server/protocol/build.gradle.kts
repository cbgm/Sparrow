plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.sparrow.lint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.bundles.serialization)
    testImplementation(kotlin("test"))
}
