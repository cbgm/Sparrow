package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object DependencyMatrixMarkdownRenderer {

    fun render(
        model: ArchitectureModel,
    ): String {
        val modules = model.modules
            .sortedBy(ArchitectureModule::path)

        return buildString {
            appendLine("# Dependency Matrix")
            appendLine()
            appendLine("A check mark means the row module directly depends on the column module.")
            appendLine()

            append("| Module |")
            modules.forEach { module ->
                append(" `${module.shortName()}` |")
            }
            appendLine()

            append("|---|")
            repeat(modules.size) {
                append("---:|")
            }
            appendLine()

            modules.forEach { source ->
                append("| `${source.path}` |")

                modules.forEach { target ->
                    val marker = if (target.path in source.dependencies) {
                        "✓"
                    } else {
                        ""
                    }

                    append(" $marker |")
                }

                appendLine()
            }
        }.trimEnd() + "\n"
    }

    private fun ArchitectureModule.shortName(): String {
        return path
            .removePrefix(":")
            .replace(":", "/")
    }
}
