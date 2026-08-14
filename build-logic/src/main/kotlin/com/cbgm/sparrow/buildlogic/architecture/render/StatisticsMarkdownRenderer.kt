package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel

internal object StatisticsMarkdownRenderer {

    fun render(
        model: ArchitectureModel,
    ): String {
        val statistics = model.statistics

        return buildString {
            appendLine("# Architecture Statistics")
            appendLine()
            appendLine("Generated automatically by `./gradlew architectureReport`.")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("| Metric | Count |")
            appendLine("|---|---:|")
            appendLine("| Modules | ${statistics.moduleCount} |")
            appendLine("| Module groups | ${statistics.groupCount} |")
            appendLine("| Dependency edges | ${statistics.dependencyCount} |")
            appendLine("| Modules without project dependencies | ${statistics.moduleWithoutDependenciesCount} |")
            appendLine("| Modules without dependents | ${statistics.moduleWithoutDependentsCount} |")
            appendLine("| Maximum direct dependencies | ${statistics.maximumDirectDependencyCount} |")
            appendLine("| Maximum direct dependents | ${statistics.maximumDirectDependentCount} |")
            appendLine("| Distinct source sets | ${statistics.sourceSetCount} |")
            appendLine("| Kotlin files | ${statistics.kotlinSourceFileCount} |")
            appendLine("| Production Kotlin files | ${statistics.productionKotlinFileCount} |")
            appendLine("| Test Kotlin files | ${statistics.testKotlinFileCount} |")
            appendLine("| Resource files | ${statistics.resourceFileCount} |")
            appendLine()
            appendLine("## Modules by group")
            appendLine()
            appendLine("| Group | Modules |")
            appendLine("|---|---:|")

            model.groups.forEach { (group, modules) ->
                appendLine("| $group | ${modules.size} |")
            }

            appendLine()
            appendLine("## Source-set usage")
            appendLine()
            appendLine("| Source set | Modules |")
            appendLine("|---|---:|")

            model.allSourceSets.forEach { sourceSet ->
                val moduleCount = model.modules.count { module ->
                    sourceSet in module.sourceSets
                }
                appendLine("| `$sourceSet` | $moduleCount |")
            }
        }.trimEnd() + "\n"
    }
}
