package com.cbgm.sparrow.buildlogic.quality

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class InstallGitHooksTask : DefaultTask() {

    @get:InputDirectory
    abstract val repositoryDirectory: DirectoryProperty

    @get:InputDirectory
    abstract val hooksDirectory: DirectoryProperty

    @get:Input
    abstract val hooksPath: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun install() {
        val repository = repositoryDirectory.get().asFile
        val hooks = hooksDirectory.get().asFile

        val gitDirectory = repository.resolve(".git")

        if (!gitDirectory.exists()) {
            logger.lifecycle(
                "Git hooks were not installed because this is not a Git checkout.",
            )
            return
        }

        if (!hooks.exists()) {
            throw GradleException(
                "Git hooks directory does not exist: ${hooks.absolutePath}",
            )
        }

        val result = execOperations.exec {
            workingDir(repository)

            commandLine(
                "git",
                "config",
                "core.hooksPath",
                hooksPath.get(),
            )

            isIgnoreExitValue = true
        }

        if (result.exitValue != 0) {
            throw GradleException(
                "Could not configure Git core.hooksPath.",
            )
        }

        logger.lifecycle(
            "Git hooks installed from ${hooksPath.get()}",
        )
    }
}
