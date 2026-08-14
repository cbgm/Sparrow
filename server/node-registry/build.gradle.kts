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
    mainClass.set("com.cbgm.securechat.server.registry.ApplicationKt")
}

dependencies {
    implementation(projects.server.protocol)
    implementation(projects.server.security)
    implementation(projects.server.persistence)
    implementation(projects.server.observability)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.coroutines)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.logback.classic)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
}
