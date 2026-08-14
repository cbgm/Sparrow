package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal fun ArchitectureModule.documentationFile(): String {
    return "modules/${documentationSlug()}.md"
}

internal fun ArchitectureModule.documentationLink(
    fromGeneratedRoot: Boolean = true,
): String {
    val prefix = if (fromGeneratedRoot) "" else "../"
    return "$prefix${documentationFile()}"
}

private fun ArchitectureModule.documentationSlug(): String {
    return path
        .removePrefix(":")
        .replace(":", "-")
        .replace(NON_SLUG_CHARACTERS, "-")
        .trim('-')
        .ifBlank { "root" }
}

private val NON_SLUG_CHARACTERS = Regex("[^A-Za-z0-9_-]")
