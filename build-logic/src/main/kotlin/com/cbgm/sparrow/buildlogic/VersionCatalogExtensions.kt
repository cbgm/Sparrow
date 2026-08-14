package com.cbgm.sparrow.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")

internal fun VersionCatalog.requiredVersion(
    alias: String,
): String {
    return findVersion(alias)
        .orElseThrow {
            IllegalStateException(
                "Version alias '$alias' is missing from libs.versions.toml",
            )
        }
        .requiredVersion
}

internal fun VersionCatalog.intVersion(
    alias: String,
): Int {
    return requiredVersion(alias).toInt()
}

internal fun VersionCatalog.requiredLibrary(
    alias: String,
): Provider<MinimalExternalModuleDependency> {
    return findLibrary(alias)
        .orElseThrow {
            IllegalStateException(
                "Library alias '$alias' is missing from libs.versions.toml",
            )
        }
}