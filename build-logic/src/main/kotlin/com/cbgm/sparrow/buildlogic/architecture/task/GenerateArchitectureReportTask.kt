package com.cbgm.sparrow.buildlogic.architecture.task

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModel
import com.cbgm.sparrow.buildlogic.architecture.render.GeneratedArchitectureFiles
import com.cbgm.sparrow.buildlogic.architecture.serialization.ArchitectureModuleCodec
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateArchitectureReportTask : DefaultTask() {

    @get:Input
    abstract val moduleDefinitions: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val model = ArchitectureModel(
            modules = moduleDefinitions
                .get()
                .map(ArchitectureModuleCodec::decode),
        )

        val directory = outputDirectory.get().asFile
        val generatedFiles = GeneratedArchitectureFiles.render(model)

        directory.deleteRecursively()
        directory.mkdirs()

        generatedFiles.forEach { (fileName, content) ->
            val file = directory.resolve(fileName)
            file.parentFile.mkdirs()
            file.writeText(content)
        }

        logger.lifecycle(
            "Generated ${generatedFiles.size} " +
                "architecture documentation files in ${directory.absolutePath}",
        )
    }
}
