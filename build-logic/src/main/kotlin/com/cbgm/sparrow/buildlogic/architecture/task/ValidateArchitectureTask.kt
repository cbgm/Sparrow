package com.cbgm.sparrow.buildlogic.architecture.task

import com.cbgm.sparrow.buildlogic.architecture.model.ArchitectureModule
import com.cbgm.sparrow.buildlogic.architecture.serialization.ArchitectureModuleCodec
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ValidateArchitectureTask : DefaultTask() {

    @get:Input
    abstract val moduleDefinitions: ListProperty<String>

    @TaskAction
    fun validate() {
        val modules = moduleDefinitions
            .get()
            .map(ArchitectureModuleCodec::decode)

        validateUniqueModulePaths(modules)
        validateKnownDependencies(modules)
        validateNoSelfDependencies(modules)
        validateNoProjectDependencyCycles(modules)

        logger.lifecycle(
            "Sparrow architecture validation passed " +
                "for ${modules.size} modules.",
        )
    }

    private fun validateUniqueModulePaths(
        modules: List<ArchitectureModule>,
    ) {
        val duplicates = modules
            .groupingBy(ArchitectureModule::path)
            .eachCount()
            .filterValues { count ->
                count > 1
            }
            .keys
            .sorted()

        if (duplicates.isEmpty()) {
            return
        }

        throw GradleException(
            buildString {
                appendLine(
                    "Duplicate architecture module paths found:",
                )

                duplicates.forEach { path ->
                    appendLine(" - $path")
                }
            },
        )
    }

    private fun validateKnownDependencies(
        modules: List<ArchitectureModule>,
    ) {
        val knownPaths = modules
            .map(ArchitectureModule::path)
            .toSet()

        val unknownDependencies = modules
            .flatMap { module ->
                module.dependencies
                    .filterNot(knownPaths::contains)
                    .map { dependencyPath ->
                        "${module.path} -> $dependencyPath"
                    }
            }
            .distinct()
            .sorted()

        if (unknownDependencies.isEmpty()) {
            return
        }

        throw GradleException(
            buildString {
                appendLine(
                    "Unknown project dependencies found:",
                )

                unknownDependencies.forEach { dependency ->
                    appendLine(" - $dependency")
                }
            },
        )
    }

    private fun validateNoSelfDependencies(
        modules: List<ArchitectureModule>,
    ) {
        val selfDependencies = modules
            .filter { module ->
                module.path in module.dependencies
            }
            .map(ArchitectureModule::path)
            .sorted()

        if (selfDependencies.isEmpty()) {
            return
        }

        throw GradleException(
            buildString {
                appendLine(
                    "Self-referencing project dependencies found:",
                )

                selfDependencies.forEach { path ->
                    appendLine(" - $path")
                }
            },
        )
    }

    private fun validateNoProjectDependencyCycles(
        modules: List<ArchitectureModule>,
    ) {
        val knownPaths = modules
            .map(ArchitectureModule::path)
            .toSet()

        val graph = modules.associate { module ->
            module.path to module.dependencies
                .filter(knownPaths::contains)
                .toSet()
        }

        val cycle = findCycle(graph)
            ?: return

        throw GradleException(
            buildString {
                appendLine(
                    "Project dependency cycle detected:",
                )
                appendLine()
                appendLine(
                    cycle.joinToString(" -> "),
                )
            },
        )
    }

    private fun findCycle(
        graph: Map<String, Set<String>>,
    ): List<String>? {
        val visited = mutableSetOf<String>()
        val active = mutableSetOf<String>()
        val stack = mutableListOf<String>()

        fun visit(
            node: String,
        ): List<String>? {
            if (node in active) {
                val cycleStart = stack.indexOf(node)

                return stack
                    .subList(
                        fromIndex = cycleStart,
                        toIndex = stack.size,
                    )
                    .toList() + node
            }

            if (!visited.add(node)) {
                return null
            }

            active += node
            stack += node

            graph[node]
                .orEmpty()
                .sorted()
                .forEach { dependency ->
                    val cycle = visit(dependency)

                    if (cycle != null) {
                        return cycle
                    }
                }

            active -= node
            stack.removeAt(stack.lastIndex)

            return null
        }

        graph.keys
            .sorted()
            .forEach { node ->
                val cycle = visit(node)

                if (cycle != null) {
                    return cycle
                }
            }

        return null
    }
}
