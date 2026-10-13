package com.cbgm.sparrow.buildlogic.workflow.render

import com.cbgm.sparrow.buildlogic.workflow.parse.WorkflowSourceParser
import java.io.File

/** Collects the workflows of every orchestration module below [orchestrationDirectory]. */
internal object WorkflowCatalogReader {

    fun read(rootDirectory: File, orchestrationDirectory: File): List<WorkflowModule> =
        moduleNames(orchestrationDirectory).map { module ->
            val sources = orchestrationDirectory.resolve("$module/src/commonMain/kotlin")
            val workflows = sources
                .walkTopDown()
                .filter { file -> file.isFile && file.name.endsWith("Workflow.kt") }
                .map { file ->
                    WorkflowSourceParser.parse(
                        source = file.readText(),
                        sourcePath = file.relativeTo(rootDirectory).invariantSeparatorsPath,
                    )
                }
                .sortedWith(compareBy({ it.folder }, { it.className }))
                .toList()
            WorkflowModule(module, workflows)
        }

    /** Known modules in layering order, then any new workflow module alphabetically. */
    private fun moduleNames(orchestrationDirectory: File): List<String> {
        val present = orchestrationDirectory
            .listFiles { file -> file.isDirectory && file.resolve("src/commonMain").isDirectory }
            .orEmpty()
            .map(File::getName)
            .filterNot { it in NON_WORKFLOW_MODULES }
        val known = WorkflowCatalogRenderer.MODULES.filter { it in present }
        return known + (present - known.toSet()).sorted()
    }

    private val NON_WORKFLOW_MODULES = setOf("api", "engine")
}
