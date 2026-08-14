package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule

internal object GeneratedArchitectureFiles {

    const val INDEX_FILE = "index.md"
    const val ARCHITECTURE_FILE = "architecture.md"
    const val MERMAID_FILE = "architecture.mmd"
    const val MODULES_FILE = "modules.md"
    const val DEPENDENCY_MATRIX_FILE = "dependency-matrix.md"
    const val STATISTICS_FILE = "statistics.md"
    const val MODULES_JSON_FILE = "modules.json"
    const val DEPENDENCIES_JSON_FILE = "dependencies.json"
    const val STATISTICS_JSON_FILE = "statistics.json"

    fun render(
        model: ArchitectureModel,
    ): Map<String, String> {
        return buildMap {
            put(INDEX_FILE, GeneratedIndexMarkdownRenderer.render(model))
            put(ARCHITECTURE_FILE, ArchitectureMarkdownRenderer.render(model))
            put(MERMAID_FILE, ArchitectureMermaidRenderer.render(model) + "\n")
            put(MODULES_FILE, ModulesMarkdownRenderer.render(model))
            put(
                DEPENDENCY_MATRIX_FILE,
                DependencyMatrixMarkdownRenderer.render(model),
            )
            put(STATISTICS_FILE, StatisticsMarkdownRenderer.render(model))
            put(MODULES_JSON_FILE, ArchitectureJsonRenderer.renderModules(model))
            put(
                DEPENDENCIES_JSON_FILE,
                ArchitectureJsonRenderer.renderDependencies(model),
            )
            put(
                STATISTICS_JSON_FILE,
                ArchitectureJsonRenderer.renderStatistics(model),
            )

            model.modules
                .sortedBy(ArchitectureModule::path)
                .forEach { module ->
                    put(
                        module.documentationFile(),
                        ModuleDetailMarkdownRenderer.render(
                            model = model,
                            module = module,
                        ),
                    )
                }
        }
    }
}
