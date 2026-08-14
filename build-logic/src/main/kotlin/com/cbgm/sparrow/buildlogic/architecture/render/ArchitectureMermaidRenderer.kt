package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object ArchitectureMermaidRenderer {

    fun render(
        model: ArchitectureModel,
    ): String {
        return buildString {
            appendLine("graph TD")
            appendLine()

            model.groups.forEach { (group, groupModules) ->
                append("    subgraph ")
                append(group.toMermaidId(prefix = "group"))
                append("[\"")
                append(group.escapeMermaid())
                appendLine("\"]")

                groupModules
                    .sortedBy(ArchitectureModule::path)
                    .forEach { module ->
                        append("        ")
                        append(module.path.toMermaidId(prefix = "module"))
                        append("[\"")
                        append(module.path.escapeMermaid())
                        appendLine("\"]")
                    }

                appendLine("    end")
                appendLine()
            }

            model.dependencies.forEach { dependency ->
                append("    ")
                append(dependency.source.toMermaidId(prefix = "module"))
                append(" --> ")
                appendLine(dependency.target.toMermaidId(prefix = "module"))
            }
        }.trimEnd()
    }

    private fun String.toMermaidId(
        prefix: String,
    ): String {
        val sanitized = removePrefix(":")
            .replace(
                regex = NON_IDENTIFIER_CHARACTERS,
                replacement = "_",
            )
            .ifBlank { "root" }

        return "${prefix}_$sanitized"
    }

    private fun String.escapeMermaid(): String {
        return replace(
            oldValue = "\"",
            newValue = "\\\"",
        )
    }

    private val NON_IDENTIFIER_CHARACTERS =
        Regex("[^A-Za-z0-9_]")
}
