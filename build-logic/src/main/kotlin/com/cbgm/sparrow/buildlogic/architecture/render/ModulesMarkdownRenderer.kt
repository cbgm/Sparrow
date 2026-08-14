package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object ModulesMarkdownRenderer {

    fun render(
        model: ArchitectureModel,
    ): String {
        return buildString {
            appendLine("# Modules")
            appendLine()
            appendLine("Generated automatically by `./gradlew architectureReport`.")
            appendLine()
            appendLine(
                "Select a module to open its generated detail page with source sets, " +
                    "file counts, dependencies and dependents.",
            )
            appendLine()
            appendLine(
                "| Module | Group | Kotlin | Tests | Resources | Dependencies | Dependents |",
            )
            appendLine("|---|---|---:|---:|---:|---:|---:|")

            model.modules
                .sortedBy(ArchitectureModule::path)
                .forEach { module ->
                    append("| [`${module.path}`](${module.documentationLink()}) | ")
                    append("`${module.group}` | ")
                    append("${module.kotlinSourceFileCount} | ")
                    append("${module.testKotlinFileCount} | ")
                    append("${module.resourceFileCount} | ")
                    append("${model.dependenciesOf(module.path).size} | ")
                    append("${model.dependentsOf(module.path).size} |")
                    appendLine()
                }
        }.trimEnd() + "\n"
    }
}
