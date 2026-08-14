package com.cbgm.sparrow.buildlogic.architecture.task

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.render.GeneratedArchitectureFiles
import com.cbgm.sparrow.buildlogic.architecture.serialization.ArchitectureModuleCodec
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class VerifyArchitectureReportTask : DefaultTask() {

    @get:Input
    abstract val moduleDefinitions: ListProperty<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedDirectory: DirectoryProperty

    @TaskAction
    fun verify() {
        val model = ArchitectureModel(
            modules = moduleDefinitions
                .get()
                .map(ArchitectureModuleCodec::decode),
        )

        val directory = generatedDirectory.get().asFile
        val expectedFiles = GeneratedArchitectureFiles.render(model)
        val staleFiles = mutableListOf<String>()

        expectedFiles.forEach { (fileName, expectedContent) ->
            val file = directory.resolve(fileName)

            if (!file.exists() || file.readText() != expectedContent) {
                staleFiles += file.relativeTo(directory).invariantSeparatorsPath
            }
        }

        val unexpectedFiles = directory
            .walkTopDown()
            .filter { file -> file.isFile }
            .map { file -> file.relativeTo(directory).invariantSeparatorsPath }
            .filterNot(expectedFiles::containsKey)
            .toList()

        if (staleFiles.isEmpty() && unexpectedFiles.isEmpty()) {
            logger.lifecycle(
                "Generated architecture documentation is up to date.",
            )
            return
        }

        throw GradleException(
            buildString {
                if (staleFiles.isNotEmpty()) {
                    appendLine("Generated architecture documentation is stale:")
                    staleFiles.sorted().forEach { path ->
                        appendLine(" - $path")
                    }
                }

                if (unexpectedFiles.isNotEmpty()) {
                    if (staleFiles.isNotEmpty()) {
                        appendLine()
                    }
                    appendLine("Unexpected generated architecture files exist:")
                    unexpectedFiles.sorted().forEach { path ->
                        appendLine(" - $path")
                    }
                }

                appendLine()
                appendLine(
                    "Run './gradlew architectureReport' and commit the changes.",
                )
            },
        )
    }
}
