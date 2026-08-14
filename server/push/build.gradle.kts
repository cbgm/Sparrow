plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization.classpath)
    alias(libs.plugins.securechat.lint)
    alias(libs.plugins.securechat.properties)
    application
}

val firebaseAdminCredentialsPath =
    localProperties.getOrNull(
        "securechat.firebase.adminCredentials"
    )

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.cbgm.securechat.server.push.ApplicationKt")
}

dependencies {
    implementation(projects.server.protocol)
    implementation(projects.server.persistence)
    implementation(projects.server.security)
    implementation(projects.server.observability)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)
    implementation(libs.bundles.coroutines)
    implementation(libs.firebase.admin)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.logback.classic)
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
}

tasks.named<JavaExec>("run") {
    if (!firebaseAdminCredentialsPath.isNullOrBlank()) {
        val credentialsFile = rootProject.file(firebaseAdminCredentialsPath)

        environment(
            "GOOGLE_APPLICATION_CREDENTIALS",
            credentialsFile.absolutePath
        )

        doFirst {
            require(credentialsFile.isFile) {
                "Firebase Admin credential file does not exist: ${credentialsFile.absolutePath}"
            }
        }
    }
}
