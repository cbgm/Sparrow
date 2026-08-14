package com.cbgm.sparrow.buildlogic.architecture.render

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel

internal object GeneratedIndexMarkdownRenderer {

    fun render(
        model: ArchitectureModel,
    ): String {
        return buildString {
            appendLine("# Generated Project Documentation")
            appendLine()
            appendLine(
                "Generated automatically by `./gradlew architectureReport`.",
            )
            appendLine()
            appendLine("| Document | Description |")
            appendLine("|---|---|")
            appendLine("| [Architecture](architecture.md) | Module graph and grouped overview |")
            appendLine("| [Modules](modules.md) | Catalog and generated module detail pages |")
            appendLine("| [Dependency matrix](dependency-matrix.md) | Matrix of direct project dependencies |")
            appendLine("| [Statistics](statistics.md) | Project graph and source statistics |")
            appendLine()
            appendLine("## Machine-readable output")
            appendLine()
            appendLine("- [`modules.json`](modules.json)")
            appendLine("- [`dependencies.json`](dependencies.json)")
            appendLine("- [`statistics.json`](statistics.json)")
            appendLine("- [`architecture.mmd`](architecture.mmd)")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("- Modules: **${model.statistics.moduleCount}**")
            appendLine("- Module groups: **${model.statistics.groupCount}**")
            appendLine("- Project dependency edges: **${model.statistics.dependencyCount}**")
            appendLine("- Kotlin files: **${model.statistics.kotlinSourceFileCount}**")
            appendLine("- Test Kotlin files: **${model.statistics.testKotlinFileCount}**")
            appendLine("- Resource files: **${model.statistics.resourceFileCount}**")
        }.trimEnd() + "\n"
    }
}
