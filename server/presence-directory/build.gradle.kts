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
    mainClass.set("com.cbgm.securechat.server.presence.ApplicationKt")
}

dependencies {
    implementation(projects.server.persistence)
    implementation(projects.server.protocol)
    implementation(projects.server.security)
    implementation(projects.server.observability)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.coroutines)
    implementation(libs.ktor.client.cio)
    implementation(libs.jedis)
    implementation(libs.logback.classic)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
}
