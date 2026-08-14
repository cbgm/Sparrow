package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object ArchitectureJsonRenderer {

    fun renderModules(
        model: ArchitectureModel,
    ): String {
        return buildString {
            appendLine("[")

            model.modules
                .sortedBy(ArchitectureModule::path)
                .forEachIndexed { index, module ->
                    appendLine("  {")
                    appendLine("    \"name\": \"${module.name.escapeJson()}\",")
                    appendLine("    \"path\": \"${module.path.escapeJson()}\",")
                    appendLine("    \"group\": \"${module.group.escapeJson()}\",")
                    appendLine("    \"directory\": \"${module.directory.escapeJson()}\",")
                    appendLine("    \"buildFile\": \"${module.buildFile.escapeJson()}\",")
                    appendLine("    \"kotlinSourceFileCount\": ${module.kotlinSourceFileCount},")
                    appendLine("    \"productionKotlinFileCount\": ${module.productionKotlinFileCount},")
                    appendLine("    \"testKotlinFileCount\": ${module.testKotlinFileCount},")
                    appendLine("    \"resourceFileCount\": ${module.resourceFileCount},")
                    appendJsonArray(
                        name = "sourceSets",
                        values = module.sourceSets.sorted(),
                        trailingComma = true,
                    )
                    appendJsonArray(
                        name = "dependencies",
                        values = model.dependenciesOf(module.path),
                        trailingComma = true,
                    )
                    appendJsonArray(
                        name = "dependents",
                        values = model.dependentsOf(module.path),
                        trailingComma = false,
                    )
                    append("  }")
                    if (index < model.modules.lastIndex) {
                        append(",")
                    }
                    appendLine()
                }

            appendLine("]")
        }
    }

    fun renderDependencies(
        model: ArchitectureModel,
    ): String {
        return buildString {
            appendLine("[")

            model.dependencies.forEachIndexed { index, dependency ->
                append("  {\"source\": \"")
                append(dependency.source.escapeJson())
                append("\", \"target\": \"")
                append(dependency.target.escapeJson())
                append("\"}")
                if (index < model.dependencies.lastIndex) {
                    append(",")
                }
                appendLine()
            }

            appendLine("]")
        }
    }

    fun renderStatistics(
        model: ArchitectureModel,
    ): String {
        val statistics = model.statistics

        return buildString {
            appendLine("{")
            appendLine("  \"moduleCount\": ${statistics.moduleCount},")
            appendLine("  \"groupCount\": ${statistics.groupCount},")
            appendLine("  \"dependencyCount\": ${statistics.dependencyCount},")
            appendLine(
                "  \"moduleWithoutDependenciesCount\": " +
                    "${statistics.moduleWithoutDependenciesCount},",
            )
            appendLine(
                "  \"moduleWithoutDependentsCount\": " +
                    "${statistics.moduleWithoutDependentsCount},",
            )
            appendLine(
                "  \"maximumDirectDependencyCount\": " +
                    "${statistics.maximumDirectDependencyCount},",
            )
            appendLine(
                "  \"maximumDirectDependentCount\": " +
                    "${statistics.maximumDirectDependentCount},",
            )
            appendLine("  \"sourceSetCount\": ${statistics.sourceSetCount},")
            appendLine("  \"kotlinSourceFileCount\": ${statistics.kotlinSourceFileCount},")
            appendLine("  \"productionKotlinFileCount\": ${statistics.productionKotlinFileCount},")
            appendLine("  \"testKotlinFileCount\": ${statistics.testKotlinFileCount},")
            appendLine("  \"resourceFileCount\": ${statistics.resourceFileCount},")
            appendLine("  \"modulesByGroup\": {")

            val groups = model.groups.entries.toList()
            groups.forEachIndexed { index, (group, modules) ->
                val suffix = if (index < groups.lastIndex) "," else ""
                appendLine(
                    "    \"${group.escapeJson()}\": ${modules.size}$suffix",
                )
            }

            appendLine("  },")
            appendLine("  \"modulesBySourceSet\": {")

            val sourceSets = model.allSourceSets.toList()
            sourceSets.forEachIndexed { index, sourceSet ->
                val suffix = if (index < sourceSets.lastIndex) "," else ""
                val moduleCount = model.modules.count { module ->
                    sourceSet in module.sourceSets
                }
                appendLine(
                    "    \"${sourceSet.escapeJson()}\": $moduleCount$suffix",
                )
            }

            appendLine("  }")
            appendLine("}")
        }
    }

    private fun StringBuilder.appendJsonArray(
        name: String,
        values: List<String>,
        trailingComma: Boolean,
    ) {
        appendLine("    \"${name.escapeJson()}\": [")
        values.forEachIndexed { index, value ->
            val suffix = if (index < values.lastIndex) "," else ""
            appendLine("      \"${value.escapeJson()}\"$suffix")
        }
        val suffix = if (trailingComma) "," else ""
        appendLine("    ]$suffix")
    }

    private fun String.escapeJson(): String {
        return buildString {
            this@escapeJson.forEach { character ->
                when (character) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(character)
                }
            }
        }
    }
}
