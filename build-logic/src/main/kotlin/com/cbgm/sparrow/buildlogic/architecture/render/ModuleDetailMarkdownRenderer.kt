package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object ModuleDetailMarkdownRenderer {

    fun render(
        model: ArchitectureModel,
        module: ArchitectureModule,
    ): String {
        val dependencies = model.dependenciesOf(module.path)
        val dependents = model.dependentsOf(module.path)

        return buildString {
            appendLine("# `${module.path}`")
            appendLine()
            appendLine("Generated automatically by `./gradlew architectureReport`.")
            appendLine()
            appendLine("## Module information")
            appendLine()
            appendLine("| Property | Value |")
            appendLine("|---|---|")
            appendLine("| Name | `${module.name}` |")
            appendLine("| Group | `${module.group}` |")
            appendLine("| Directory | `${module.directory}` |")
            appendLine("| Build file | `${module.buildFile}` |")
            appendLine("| Kotlin files | ${module.kotlinSourceFileCount} |")
            appendLine("| Production Kotlin files | ${module.productionKotlinFileCount} |")
            appendLine("| Test Kotlin files | ${module.testKotlinFileCount} |")
            appendLine("| Resource files | ${module.resourceFileCount} |")
            appendLine("| Direct dependencies | ${dependencies.size} |")
            appendLine("| Direct dependents | ${dependents.size} |")
            appendLine()

            appendLine("## Source sets")
            appendLine()
            if (module.sourceSets.isEmpty()) {
                appendLine("None discovered.")
            } else {
                module.sourceSets.sorted().forEach { sourceSet ->
                    appendLine("- `$sourceSet`")
                }
            }
            appendLine()

            appendModuleLinks(
                title = "Dependencies",
                modulePaths = dependencies,
                model = model,
            )
            appendModuleLinks(
                title = "Dependents",
                modulePaths = dependents,
                model = model,
            )
        }.trimEnd() + "\n"
    }

    private fun StringBuilder.appendModuleLinks(
        title: String,
        modulePaths: List<String>,
        model: ArchitectureModel,
    ) {
        appendLine("## $title")
        appendLine()

        if (modulePaths.isEmpty()) {
            appendLine("None.")
            appendLine()
            return
        }

        modulePaths.forEach { modulePath ->
            val relatedModule = model.modulesByPath.getValue(modulePath)
            appendLine(
                "- [`$modulePath`](${relatedModule.documentationLink(fromGeneratedRoot = false)})",
            )
        }
        appendLine()
    }
}
