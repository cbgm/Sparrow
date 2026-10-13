package com.cbgm.sparrow.buildlogic.workflow.task

import com.cbgm.sparrow.buildlogic.workflow.render.WorkflowCatalogReader
import com.cbgm.sparrow.buildlogic.workflow.render.WorkflowCatalogRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes `docs/architecture/workflows` from the conversation orchestration workflow sources. */
abstract class GenerateWorkflowCatalogTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val workflowSources: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    abstract val orchestrationDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val modules = WorkflowCatalogReader.read(
            rootDirectory = rootDirectory.get().asFile,
            orchestrationDirectory = orchestrationDirectory.get().asFile,
        )
        val files = WorkflowCatalogRenderer.render(modules)
        val directory = outputDirectory.get().asFile

        directory.deleteRecursively()
        directory.mkdirs()
        files.forEach { (name, content) -> directory.resolve(name).writeText(content) }

        logger.lifecycle(
            "Generated workflow catalog for ${modules.sumOf { it.workflows.size }} workflows in ${directory.absolutePath}",
        )
    }
}
