package com.cbgm.sparrow.buildlogic.workflow.task

import com.cbgm.sparrow.buildlogic.workflow.render.WorkflowCatalogReader
import com.cbgm.sparrow.buildlogic.workflow.render.WorkflowCatalogRenderer
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Fails when `docs/architecture/workflows` no longer matches the workflow sources. */
abstract class VerifyWorkflowCatalogTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val workflowSources: ConfigurableFileCollection

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Internal
    abstract val orchestrationDirectory: DirectoryProperty

    /** Read directly instead of as an input, so this check may run in the same build as `workflowCatalog`. */
    @get:Internal
    abstract val catalogDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        val expected = WorkflowCatalogRenderer.render(
            WorkflowCatalogReader.read(rootDirectory.get().asFile, orchestrationDirectory.get().asFile),
        )
        val directory = catalogDirectory.get().asFile
        val stale = expected.filter { (name, content) ->
            val file = directory.resolve(name)
            !file.isFile || file.readText().replace("\r\n", "\n") != content
        }.keys
        val unexpected = directory.listFiles().orEmpty().map { it.name }.filterNot(expected::containsKey)

        if (stale.isEmpty() && unexpected.isEmpty()) {
            logger.lifecycle("Workflow catalog is up to date.")
            return
        }
        throw GradleException(
            buildString {
                appendLine("The workflow catalog in ${directory.path} is out of date:")
                (stale + unexpected).sorted().forEach { appendLine(" - $it") }
                appendLine()
                appendLine("Run './gradlew workflowCatalog' and commit the changes.")
            },
        )
    }
}
