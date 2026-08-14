package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object ArchitectureMarkdownRenderer {

    fun render(
        model: ArchitectureModel,
    ): String {
        return buildString {
            appendLine("# Sparrow Architecture")
            appendLine()
            appendLine("Generated automatically by `./gradlew architectureReport`.")
            appendLine()

            appendOverview(model)
            appendGroups(model.groups)
            appendGraph(model)
        }.trimEnd() + "\n"
    }

    private fun StringBuilder.appendOverview(
        model: ArchitectureModel,
    ) {
        appendLine("## Overview")
        appendLine()
        appendLine("| Metric | Count |")
        appendLine("|---|---:|")
        appendLine("| Modules | ${model.statistics.moduleCount} |")
        appendLine("| Module groups | ${model.statistics.groupCount} |")
        appendLine("| Project dependencies | ${model.statistics.dependencyCount} |")
        appendLine("| Kotlin files | ${model.statistics.kotlinSourceFileCount} |")
        appendLine("| Test Kotlin files | ${model.statistics.testKotlinFileCount} |")
        appendLine("| Resource files | ${model.statistics.resourceFileCount} |")
        appendLine()
    }

    private fun StringBuilder.appendGroups(
        modulesByGroup: Map<String, List<ArchitectureModule>>,
    ) {
        appendLine("## Module groups")
        appendLine()

        modulesByGroup.forEach { (group, modules) ->
            appendLine("### $group")
            appendLine()

            modules
                .sortedBy(ArchitectureModule::path)
                .forEach { module ->
                    appendLine(
                        "- [**${module.name}** (`${module.path}`)](${module.documentationLink()})",
                    )
                }

            appendLine()
        }
    }

    private fun StringBuilder.appendGraph(
        model: ArchitectureModel,
    ) {
        appendLine("## Module graph")
        appendLine()
        appendLine("```mermaid")
        appendLine(ArchitectureMermaidRenderer.render(model))
        appendLine("```")
        appendLine()
    }
}
