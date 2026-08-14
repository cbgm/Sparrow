import java.util.Properties

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.about.libs) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.securechat.architecture)
    alias(libs.plugins.securechat.quality)
}


val localProperties =
    Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")

        if (localPropertiesFile.exists()) {
            localPropertiesFile
                .inputStream()
                .use(::load)
        }
    }

fun localProperty(
    key: String,
    defaultValue: String
): String =
    localProperties.getProperty(
        key,
        defaultValue
    )

fun String.asBuildConfigValue(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
