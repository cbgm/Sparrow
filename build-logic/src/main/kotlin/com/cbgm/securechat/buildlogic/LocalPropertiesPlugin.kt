package com.cbgm.securechat.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

open class LocalPropertiesExtension(
    project: Project
) {
    private val providers = project.providers

    private val properties =
        Properties().apply {
            val file =
                project.rootProject.file("local.properties")

            if (file.exists()) {
                file.inputStream().use(::load)
            }
        }

    fun get(
        key: String,
        defaultValue: String
    ): String =
        providers
            .gradleProperty(key)
            .orNull
            ?.takeIf(String::isNotBlank)
            ?: properties.getProperty(
                key,
                defaultValue
            )

    fun getOrNull(
        key: String
    ): String? =
        providers
            .gradleProperty(key)
            .orNull
            ?.takeIf(String::isNotBlank)
            ?: properties
                .getProperty(key)
                ?.takeIf(String::isNotBlank)

    fun buildConfigString(
        key: String,
        defaultValue: String
    ): String {
        val value =
            get(
                key = key,
                defaultValue = defaultValue
            )

        return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
}

class LocalPropertiesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create(
            "localProperties",
            LocalPropertiesExtension::class.java,
            target
        )
    }
}
