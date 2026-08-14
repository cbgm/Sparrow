package com.cbgm.sparrow.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ValidateFeatureDependenciesTask : DefaultTask() {

    @get:Input
    abstract val modulePath: ListProperty<String>

    @get:Input
    abstract val actualFeatureDependencies: ListProperty<String>

    @get:Input
    abstract val allowedFeatureDependencies: ListProperty<String>

    @TaskAction
    fun validate() {
        val currentModule =
            modulePath.get().single()

        val allowed =
            allowedFeatureDependencies.get().toSet()

        val forbidden = actualFeatureDependencies
            .get()
            .distinct()
            .sorted()
            .filterNot(allowed::contains)

        if (forbidden.isEmpty()) {
            return
        }

        throw GradleException(
            buildString {
                appendLine(
                    "Invalid Sparrow feature dependencies in $currentModule:",
                )

                forbidden.forEach { dependency ->
                    appendLine(" - $dependency")
                }

                appendLine()
                appendLine("Allowed feature dependencies:")

                if (allowed.isEmpty()) {
                    appendLine(" - none")
                } else {
                    allowed
                        .sorted()
                        .forEach { dependency ->
                            appendLine(" - $dependency")
                        }
                }
            },
        )
    }
}
